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
import java.util.Map;

/**
 * Consumidor de la cola de facturación y tickets.
 * <p>
 * Simula la emisión de un comprobante de pago y confirma manualmente.
 */
@Component
public class FacturacionTicketsConsumer {

    private static final Logger log = LoggerFactory.getLogger(FacturacionTicketsConsumer.class);

    @RabbitListener(queues = RabbitMQConstants.FACTURACION_TICKETS_QUEUE)
    public void procesarFacturacion(Map<String, Object> mensaje,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("📥 [FACTURACIÓN] Mensaje recibido: {}", mensaje);

            // Simulación de emisión de comprobante
            Object monto = mensaje.getOrDefault("monto", 0);
            Object pedidoId = mensaje.getOrDefault("pedidoId", "N/A");
            log.info("🧾 [FACTURACIÓN] Emitiendo comprobante para Pedido #{} por monto ${}", pedidoId, monto);

            // ACK manual: comprobante generado correctamente
            channel.basicAck(deliveryTag, false);
            log.info("✅ [FACTURACIÓN] Comprobante emitido y confirmado.");

        } catch (Exception e) {
            log.error("❌ [FACTURACIÓN] Error emitiendo comprobante: {}. Derivando a DLQ.", e.getMessage());
            // NACK sin requeue → DLQ
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
