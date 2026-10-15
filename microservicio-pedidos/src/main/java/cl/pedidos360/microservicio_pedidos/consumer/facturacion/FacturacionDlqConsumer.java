package cl.pedidos360.microservicio_pedidos.consumer.facturacion;

import cl.pedidos360.microservicio_pedidos.config.RabbitMQConstants;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Inspector de la Dead Letter Queue de facturación.
 */
@Component
public class FacturacionDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(FacturacionDlqConsumer.class);

    @RabbitListener(queues = RabbitMQConstants.FACTURACION_TICKETS_DLQ)
    public void procesarMensajeFallido(Map<String, Object> mensaje,
                                        Channel channel,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                        @Header(name = "x-death", required = false) List<Map<String, Object>> xDeath) throws IOException {
        try {
            log.warn("💀 [DLQ-FACTURACIÓN] Mensaje fallido recibido: {}", mensaje);

            if (xDeath != null && !xDeath.isEmpty()) {
                Map<String, Object> deathInfo = xDeath.get(0);
                log.warn("💀 [DLQ-FACTURACIÓN] Razón: {} | Cola origen: {} | Exchange: {}",
                        deathInfo.get("reason"),
                        deathInfo.get("queue"),
                        deathInfo.get("exchange"));
            }

            log.warn("💀 [DLQ-FACTURACIÓN] ⚠ Factura descartada registrada para auditoría.");

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("💀 [DLQ-FACTURACIÓN] Error procesando mensaje DLQ: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
