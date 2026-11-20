package cl.pedidos360.microservicio_kafka_admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.pedidos360.microservicio_kafka_admin.dto.ClusterInfoDto;
import cl.pedidos360.microservicio_kafka_admin.dto.ConsumerGroupDto;
import cl.pedidos360.microservicio_kafka_admin.dto.ConsumerGroupSummaryDto;
import cl.pedidos360.microservicio_kafka_admin.dto.CreateTopicRequest;
import cl.pedidos360.microservicio_kafka_admin.dto.IncreasePartitionsRequest;
import cl.pedidos360.microservicio_kafka_admin.dto.TopicInfoDto;
import cl.pedidos360.microservicio_kafka_admin.dto.UpdateTopicConfigRequest;
import cl.pedidos360.microservicio_kafka_admin.service.KafkaAdminService;
import jakarta.validation.Valid;

/**
 * API de administracion de Kafka. Todas las operaciones requieren rol ADMIN
 * (JWT de Azure Entra ID validado por Spring Security).
 */
@RestController
@RequestMapping("/api/kafka/admin")
@PreAuthorize("hasRole('ADMIN')")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    // ---------- Cluster ----------

    @GetMapping("/cluster")
    public ResponseEntity<ClusterInfoDto> cluster() {
        return ResponseEntity.ok(service.describirCluster());
    }

    // ---------- Topicos ----------

    @GetMapping("/topics")
    public ResponseEntity<List<String>> listarTopics(@RequestParam(defaultValue = "false") boolean incluirInternos) {
        return ResponseEntity.ok(service.listarTopics(incluirInternos));
    }

    @GetMapping("/topics/{name}")
    public ResponseEntity<TopicInfoDto> describirTopic(@PathVariable String name) {
        return ResponseEntity.ok(service.describirTopic(name));
    }

    @PostMapping("/topics")
    public ResponseEntity<Map<String, Object>> crearTopic(@Valid @RequestBody CreateTopicRequest request) {
        service.crearTopic(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Topico creado exitosamente",
                "topic", request.name(),
                "partitions", request.partitions(),
                "replicationFactor", request.replicationFactor()));
    }

    @PostMapping("/topics/bootstrap")
    public ResponseEntity<List<Map<String, Object>>> crearTopicsConfigurados() {
        return ResponseEntity.ok(service.crearTopicsConfigurados());
    }

    @PutMapping("/topics/{name}/config")
    public ResponseEntity<TopicInfoDto> actualizarConfig(@PathVariable String name,
                                                         @Valid @RequestBody UpdateTopicConfigRequest request) {
        return ResponseEntity.ok(service.actualizarConfiguracion(name, request.configs()));
    }

    @PutMapping("/topics/{name}/partitions")
    public ResponseEntity<TopicInfoDto> aumentarParticiones(@PathVariable String name,
                                                            @Valid @RequestBody IncreasePartitionsRequest request) {
        return ResponseEntity.ok(service.aumentarParticiones(name, request.totalPartitions()));
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Map<String, String>> eliminarTopic(@PathVariable String name) {
        service.eliminarTopic(name);
        return ResponseEntity.ok(Map.of("message", "Topico eliminado exitosamente", "topic", name));
    }

    // ---------- Consumer groups ----------

    @GetMapping("/consumer-groups")
    public ResponseEntity<List<ConsumerGroupSummaryDto>> listarGrupos() {
        return ResponseEntity.ok(service.listarGrupos());
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ResponseEntity<ConsumerGroupDto> describirGrupo(@PathVariable String groupId) {
        return ResponseEntity.ok(service.describirGrupo(groupId));
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    public ResponseEntity<Map<String, String>> eliminarGrupo(@PathVariable String groupId) {
        service.eliminarGrupo(groupId);
        return ResponseEntity.ok(Map.of("message", "Consumer group eliminado exitosamente", "groupId", groupId));
    }
}
