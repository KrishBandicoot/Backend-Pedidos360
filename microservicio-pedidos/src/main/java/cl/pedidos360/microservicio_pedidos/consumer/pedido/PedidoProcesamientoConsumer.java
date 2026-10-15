package cl.pedidos360.microservicio_pedidos.consumer.pedido;

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
 * Consumidor principal de la cola de procesamiento de pedidos.
 * <p>
 * Implementa confirmación manual explícita (ACK/NACK):
 * <ul>
 *   <li>{@code basicAck(tag, false)} tras ejecución correcta.</li>
 *   <li>{@code basicNack(tag, false, false)} sin reencolamiento → se desvía a la DLQ.</li>
 * </ul>
 */
@Component
public class PedidoProcesamientoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoProcesamientoConsumer.class);

    @RabbitListener(queues = RabbitMQConstants.PEDIDOS_PROCESAMIENTO_QUEUE)
    public void procesarPedido(Map<String, Object> mensaje,
                                Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("📥 [PEDIDOS] Mensaje recibido: {}", mensaje);

            // Validación de negocio: si el monto o el pedidoId son inválidos, forzar error
            Object montoObj = mensaje.get("monto");
            Object pedidoIdObj = mensaje.get("pedidoId");

            if (montoObj != null && ((Number) montoObj).doubleValue() <= 0) {
                throw new RuntimeException("Monto inválido (<= 0). Pedido rechazado.");
            }
            if (pedidoIdObj != null && ((Number) pedidoIdObj).longValue() <= 0) {
                throw new RuntimeException("PedidoId inválido (<= 0). Pedido rechazado.");
            }

            // Lógica de negocio simulada
            log.info("✅ [PEDIDOS] Pedido procesado correctamente. ID: {}", pedidoIdObj);

            // ACK manual: confirmar al broker que el mensaje fue procesado exitosamente
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("❌ [PEDIDOS] Error procesando pedido: {}. Enviando a DLQ.", e.getMessage());
            // NACK manual: requeue=false → el broker envía el mensaje a la Dead Letter Queue
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
