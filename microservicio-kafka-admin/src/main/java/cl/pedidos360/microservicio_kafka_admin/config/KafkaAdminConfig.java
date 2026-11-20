package cl.pedidos360.microservicio_kafka_admin.config;

import org.apache.kafka.clients.admin.Admin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

/** Cliente Admin de Kafka compartido, con la configuracion de spring.kafka.*. */
@Configuration
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public Admin kafkaAdminClient(KafkaAdmin kafkaAdmin) {
        return Admin.create(kafkaAdmin.getConfigurationProperties());
    }
}
