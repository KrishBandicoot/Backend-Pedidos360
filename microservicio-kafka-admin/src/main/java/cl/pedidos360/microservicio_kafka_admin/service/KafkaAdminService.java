package cl.pedidos360.microservicio_kafka_admin.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import cl.pedidos360.microservicio_kafka_admin.config.KafkaAdminProperties;
import cl.pedidos360.microservicio_kafka_admin.config.KafkaAdminProperties.TopicSpec;
import cl.pedidos360.microservicio_kafka_admin.dto.ClusterInfoDto;
import cl.pedidos360.microservicio_kafka_admin.dto.ConsumerGroupDto;
import cl.pedidos360.microservicio_kafka_admin.dto.ConsumerGroupSummaryDto;
import cl.pedidos360.microservicio_kafka_admin.dto.CreateTopicRequest;
import cl.pedidos360.microservicio_kafka_admin.dto.TopicInfoDto;
import cl.pedidos360.microservicio_kafka_admin.exception.KafkaAdminException;

/**
 * Capa de gestion sobre Kafka usando el cliente Admin oficial:
 * topicos (crear, listar, describir, modificar configuracion, aumentar particiones, eliminar)
 * y consumer groups (listar, describir con lag, eliminar).
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);
    private static final long TIMEOUT_SEGUNDOS = 15;

    /** Configuraciones de topico que este servicio permite ver y modificar. */
    public static final Set<String> CONFIGS_PERMITIDAS = Set.of(
            "cleanup.policy", "retention.ms", "retention.bytes", "min.insync.replicas",
            "segment.ms", "segment.bytes", "max.message.bytes", "compression.type", "delete.retention.ms");

    private final Admin admin;
    private final KafkaAdminProperties properties;

    public KafkaAdminService(Admin admin, KafkaAdminProperties properties) {
        this.admin = admin;
        this.properties = properties;
    }

    // ======================== CLUSTER ========================

    public ClusterInfoDto describirCluster() {
        DescribeClusterResult r = admin.describeCluster();
        String clusterId = esperar(r.clusterId(), "describir cluster");
        Node controller = esperar(r.controller(), "describir controlador");
        Collection<Node> nodes = esperar(r.nodes(), "listar brokers");
        List<ClusterInfoDto.BrokerDto> brokers = nodes.stream()
                .map(n -> new ClusterInfoDto.BrokerDto(n.id(), n.host(), n.port()))
                .sorted((a, b) -> Integer.compare(a.id(), b.id()))
                .toList();
        return new ClusterInfoDto(clusterId, controller == null ? null : controller.id(), brokers);
    }

    // ======================== TOPICOS ========================

    public List<String> listarTopics(boolean incluirInternos) {
        var listings = esperar(admin.listTopics(new ListTopicsOptions().listInternal(incluirInternos)).listings(),
                "listar topicos");
        return listings.stream().map(l -> l.name()).sorted().toList();
    }

    public TopicInfoDto describirTopic(String name) {
        validarNombre(name);
        TopicDescription td = esperar(admin.describeTopics(List.of(name)).allTopicNames(), "describir topico")
                .get(name);
        if (td == null) {
            throw new KafkaAdminException(HttpStatus.NOT_FOUND, "El topico no existe: " + name);
        }

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Config config = esperar(admin.describeConfigs(List.of(resource)).all(), "leer configuracion").get(resource);
        Map<String, String> configs = new TreeMap<>();
        if (config != null) {
            for (ConfigEntry e : config.entries()) {
                if (CONFIGS_PERMITIDAS.contains(e.name())) {
                    configs.put(e.name(), String.valueOf(e.value()));
                }
            }
        }

        List<TopicInfoDto.PartitionDto> partitions = new ArrayList<>();
        for (TopicPartitionInfo p : td.partitions()) {
            partitions.add(new TopicInfoDto.PartitionDto(
                    p.partition(),
                    p.leader() == null ? -1 : p.leader().id(),
                    p.replicas().stream().map(Node::id).toList(),
                    p.isr().stream().map(Node::id).toList()));
        }
        int rf = td.partitions().isEmpty() ? 0 : td.partitions().get(0).replicas().size();
        return new TopicInfoDto(td.name(), td.isInternal(), td.partitions().size(), rf, partitions, configs);
    }

    public void crearTopic(CreateTopicRequest req) {
        validarNombre(req.name());
        Map<String, String> configs = req.configs() == null ? Map.of() : req.configs();
        validarConfigs(configs);
        crear(req.name(), req.partitions(), req.replicationFactor().shortValue(), configs);
    }

    public TopicInfoDto actualizarConfiguracion(String name, Map<String, String> configs) {
        validarNombre(name);
        validarConfigs(configs);
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        List<AlterConfigOp> ops = configs.entrySet().stream()
                .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .toList();
        esperar(admin.incrementalAlterConfigs(Map.of(resource, ops)).all(), "modificar configuracion");
        log.info("🔧 [KAFKA-ADMIN] Configuracion de {} actualizada: {}", name, configs);
        return describirTopic(name);
    }

    public TopicInfoDto aumentarParticiones(String name, int total) {
        validarNombre(name);
        // Kafka solo permite aumentar; ademas cambia a que particion van las keys nuevas.
        esperar(admin.createPartitions(Map.of(name, NewPartitions.increaseTo(total))).all(), "aumentar particiones");
        log.info("🔧 [KAFKA-ADMIN] {} ahora tiene {} particiones", name, total);
        return describirTopic(name);
    }

    public void eliminarTopic(String name) {
        validarNombre(name);
        if (name.startsWith("__")) {
            throw new KafkaAdminException(HttpStatus.BAD_REQUEST, "No se pueden eliminar topicos internos de Kafka");
        }
        esperar(admin.deleteTopics(List.of(name)).all(), "eliminar topico");
        log.info("🗑️ [KAFKA-ADMIN] Topico eliminado: {}", name);
    }

    /** Crea los topicos declarados en application.yml que aun no existan (idempotente). */
    public List<Map<String, Object>> crearTopicsConfigurados() {
        Set<String> existentes = Set.copyOf(listarTopics(false));
        List<Map<String, Object>> resultados = new ArrayList<>();
        for (TopicSpec spec : properties.getTopics()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("topic", spec.getName());
            if (existentes.contains(spec.getName())) {
                r.put("resultado", "YA_EXISTIA");
            } else {
                crear(spec.getName(), spec.getPartitions(), spec.getReplicationFactor(), spec.getConfigs());
                r.put("resultado", "CREADO");
            }
            resultados.add(r);
        }
        return resultados;
    }

    private void crear(String name, int partitions, short rf, Map<String, String> configs) {
        NewTopic topic = new NewTopic(name, partitions, rf).configs(new HashMap<>(configs));
        esperar(admin.createTopics(List.of(topic)).all(), "crear topico");
        log.info("✅ [KAFKA-ADMIN] Topico creado: {} (particiones={}, replicas={}, configs={})",
                name, partitions, rf, configs);
    }

    // ======================== CONSUMER GROUPS ========================

    public List<ConsumerGroupSummaryDto> listarGrupos() {
        return esperar(admin.listConsumerGroups().all(), "listar consumer groups").stream()
                .map(g -> new ConsumerGroupSummaryDto(g.groupId(), g.state().map(Object::toString).orElse("UNKNOWN")))
                .sorted((a, b) -> a.groupId().compareTo(b.groupId()))
                .toList();
    }

    public ConsumerGroupDto describirGrupo(String groupId) {
        ConsumerGroupDescription d = esperar(admin.describeConsumerGroups(List.of(groupId)).all(),
                "describir consumer group").get(groupId);
        if (d == null || "DEAD".equals(d.state().toString())) {
            throw new KafkaAdminException(HttpStatus.NOT_FOUND, "El consumer group no existe: " + groupId);
        }

        List<ConsumerGroupDto.MemberDto> members = new ArrayList<>();
        for (MemberDescription m : d.members()) {
            List<String> parts = m.assignment().topicPartitions().stream()
                    .map(tp -> tp.topic() + "-" + tp.partition()).sorted().toList();
            members.add(new ConsumerGroupDto.MemberDto(m.consumerId(), m.clientId(), m.host(), parts));
        }

        Map<TopicPartition, OffsetAndMetadata> committed = esperar(
                admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata(), "leer offsets del grupo");
        Map<TopicPartition, OffsetSpec> latest = new HashMap<>();
        committed.forEach((tp, om) -> {
            if (om != null) {
                latest.put(tp, OffsetSpec.latest());
            }
        });
        Map<TopicPartition, ListOffsetsResultInfo> ends = latest.isEmpty()
                ? Collections.emptyMap()
                : esperar(admin.listOffsets(latest).all(), "leer offsets finales");

        List<ConsumerGroupDto.OffsetLagDto> offsets = new ArrayList<>();
        long totalLag = 0;
        for (TopicPartition tp : latest.keySet()) {
            long current = committed.get(tp).offset();
            long end = ends.get(tp).offset();
            long lag = Math.max(0, end - current);
            totalLag += lag;
            offsets.add(new ConsumerGroupDto.OffsetLagDto(tp.topic(), tp.partition(), current, end, lag));
        }
        offsets.sort((a, b) -> a.topic().equals(b.topic())
                ? Integer.compare(a.partition(), b.partition()) : a.topic().compareTo(b.topic()));

        return new ConsumerGroupDto(groupId, d.state().toString(),
                d.coordinator() == null ? null : d.coordinator().id(), members, offsets, totalLag);
    }

    public void eliminarGrupo(String groupId) {
        esperar(admin.deleteConsumerGroups(List.of(groupId)).all(), "eliminar consumer group");
        log.info("🗑️ [KAFKA-ADMIN] Consumer group eliminado: {}", groupId);
    }

    // ======================== UTILIDADES ========================

    private void validarNombre(String name) {
        if (name == null || !name.matches("^[a-zA-Z0-9._-]{1,249}$")) {
            throw new KafkaAdminException(HttpStatus.BAD_REQUEST, "Nombre de topico invalido: " + name);
        }
    }

    private void validarConfigs(Map<String, String> configs) {
        for (String key : configs.keySet()) {
            if (!CONFIGS_PERMITIDAS.contains(key)) {
                throw new KafkaAdminException(HttpStatus.BAD_REQUEST,
                        "Configuracion no permitida: " + key + ". Permitidas: " + new TreeSet<>(CONFIGS_PERMITIDAS));
            }
        }
    }

    /** Espera un KafkaFuture y traduce los errores de Kafka a HTTP status. */
    private <T> T esperar(KafkaFuture<T> future, String operacion) {
        try {
            return future.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminException(HttpStatus.SERVICE_UNAVAILABLE, "Operacion interrumpida: " + operacion, e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new KafkaAdminException(HttpStatus.GATEWAY_TIMEOUT, "Kafka no respondio a tiempo: " + operacion, e);
        } catch (ExecutionException e) {
            throw traducir(e.getCause(), operacion);
        }
    }

    private KafkaAdminException traducir(Throwable c, String operacion) {
        String msg = operacion + ": " + (c == null ? "error desconocido" : c.getMessage());
        if (c instanceof TopicExistsException) {
            return new KafkaAdminException(HttpStatus.CONFLICT, "El topico ya existe. " + msg, c);
        }
        if (c instanceof UnknownTopicOrPartitionException || c instanceof GroupIdNotFoundException) {
            return new KafkaAdminException(HttpStatus.NOT_FOUND, "No encontrado. " + msg, c);
        }
        if (c instanceof GroupNotEmptyException) {
            return new KafkaAdminException(HttpStatus.CONFLICT, "El grupo tiene consumidores activos. " + msg, c);
        }
        if (c instanceof InvalidReplicationFactorException || c instanceof InvalidPartitionsException
                || c instanceof InvalidTopicException || c instanceof InvalidConfigurationException
                || c instanceof InvalidRequestException) {
            return new KafkaAdminException(HttpStatus.BAD_REQUEST, msg, c);
        }
        if (c instanceof org.apache.kafka.common.errors.TimeoutException) {
            return new KafkaAdminException(HttpStatus.GATEWAY_TIMEOUT, "Kafka no disponible. " + msg, c);
        }
        return new KafkaAdminException(HttpStatus.BAD_GATEWAY, msg, c);
    }
}
