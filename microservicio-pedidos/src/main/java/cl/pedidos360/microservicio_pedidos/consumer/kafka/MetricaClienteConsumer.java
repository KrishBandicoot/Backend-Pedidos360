package cl.pedidos360.microservicio_pedidos.consumer.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.pedidos360.microservicio_pedidos.config.KafkaConstants;
import cl.pedidos360.microservicio_pedidos.dto.MetricaClienteDto;
import cl.pedidos360.microservicio_pedidos.service.AnaliticaCacheService;

/**
 * Consume las métricas calculadas por microservicio-analitica.
 * Solo recibe y delega: la lógica vive en {@link AnaliticaCacheService}.
 */
@Component
public class MetricaClienteConsumer {

    private static final Logger log = LoggerFactory.getLogger(MetricaClienteConsumer.class);

    private final AnaliticaCacheService cacheService;

    public MetricaClienteConsumer(AnaliticaCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @KafkaListener(id = "pedidos-metricas-listener",
            topics = KafkaConstants.TOPIC_PEDIDOS_ANALITICA,
            groupId = KafkaConstants.GROUP_PEDIDOS_METRICAS)
    public void onMetrica(ConsumerRecord<String, MetricaClienteDto> record) {
        log.info("📥 [KAFKA] Métrica recibida {}-{}@{} key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        cacheService.actualizar(record.value());
    }
}
