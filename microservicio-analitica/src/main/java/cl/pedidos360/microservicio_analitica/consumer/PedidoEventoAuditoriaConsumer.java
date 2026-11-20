package cl.pedidos360.microservicio_analitica.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.pedidos360.microservicio_analitica.config.KafkaConstants;
import cl.pedidos360.microservicio_analitica.dto.PedidoEventoDto;

/**
 * Segundo consumer group sobre el MISMO topico: al tener otro group.id recibe todos
 * los eventos, independiente del avance de analitica-metricas-group.
 */
@Component
public class PedidoEventoAuditoriaConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventoAuditoriaConsumer.class);

    @KafkaListener(id = "analitica-auditoria-listener",
            topics = KafkaConstants.TOPIC_PEDIDOS_EVENTOS,
            groupId = KafkaConstants.GROUP_AUDITORIA)
    public void auditar(ConsumerRecord<String, PedidoEventoDto> record) {
        PedidoEventoDto e = record.value();
        log.info("🧾 [AUDITORIA] {}-{}@{} tipo={} pedido={} cliente={}",
                record.topic(), record.partition(), record.offset(),
                e == null ? null : e.tipo(), e == null ? null : e.pedidoId(), e == null ? null : e.cliente());
    }
}
