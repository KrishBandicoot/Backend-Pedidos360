package cl.pedidos360.microservicio_pedidos.producer;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import cl.pedidos360.microservicio_pedidos.config.KafkaConstants;
import cl.pedidos360.microservicio_pedidos.dto.PedidoEventoDto;
import cl.pedidos360.microservicio_pedidos.dto.TipoEventoPedido;
import cl.pedidos360.microservicio_pedidos.model.Pedido;

/**
 * Productor Kafka de eventos de pedido.
 * <p>
 * La clave del mensaje es el id del pedido: todos los eventos de un mismo pedido
 * caen en la misma partición y conservan su orden.
 * Un fallo de Kafka NO debe romper la API REST: se registra en logs y se continúa.
 */
@Service
public class PedidoKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaProducer.class);

    private final KafkaTemplate<String, PedidoEventoDto> kafkaTemplate;

    public PedidoKafkaProducer(KafkaTemplate<String, PedidoEventoDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(TipoEventoPedido tipo, Pedido pedido) {
        PedidoEventoDto evento = new PedidoEventoDto(
                UUID.randomUUID().toString(), tipo, pedido.getId(),
                pedido.getCliente(), pedido.getEstado(), Instant.now());
        String key = String.valueOf(pedido.getId());

        try {
            kafkaTemplate.send(KafkaConstants.TOPIC_PEDIDOS_EVENTOS, key, evento)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("❌ [KAFKA] No se pudo publicar {} del pedido {}: {}",
                                    tipo, key, ex.getMessage());
                        } else if (result != null) {
                            log.info("▶ [KAFKA] {} pedido={} → {}-{}@{}", tipo, key,
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            // Ej: broker caído y max.block.ms agotado
            log.error("❌ [KAFKA] Error enviando {} del pedido {}: {}", tipo, key, e.getMessage());
        }
    }
}
