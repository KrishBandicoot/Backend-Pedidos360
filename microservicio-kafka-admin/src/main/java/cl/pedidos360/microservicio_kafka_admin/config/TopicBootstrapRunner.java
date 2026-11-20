package cl.pedidos360.microservicio_kafka_admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.pedidos360.microservicio_kafka_admin.service.KafkaAdminService;

/** Crea los topicos de application.yml al arrancar. Si Kafka esta caido, el servicio igual inicia. */
@Component
@ConditionalOnProperty(name = "kafka-admin.bootstrap-topics-on-startup", havingValue = "true")
public class TopicBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TopicBootstrapRunner.class);

    private final KafkaAdminService adminService;

    public TopicBootstrapRunner(KafkaAdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            adminService.crearTopicsConfigurados()
                    .forEach(r -> log.info("🧩 [KAFKA-ADMIN] {} → {}", r.get("topic"), r.get("resultado")));
        } catch (Exception e) {
            log.warn("⚠ [KAFKA-ADMIN] No se pudieron crear los topicos al iniciar: {}. "
                    + "Usa POST /api/kafka/admin/topics/bootstrap cuando Kafka este disponible.", e.getMessage());
        }
    }
}
