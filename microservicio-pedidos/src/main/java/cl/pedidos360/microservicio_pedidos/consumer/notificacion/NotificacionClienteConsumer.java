package cl.pedidos360.microservicio_pedidos.consumer.notificacion;

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
 * Consumidor de la cola de notificaciones al cliente.
 * <p>
 * Simula el envío de correo/SMS y confirma manualmente con ACK/NACK.
 */
@Component
public class NotificacionClienteConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacionClienteConsumer.class);

    @RabbitListener(queues = RabbitMQConstants.NOTIFICACIONES_CLIENTE_QUEUE)
    public void procesarNotificacion(Map<String, Object> mensaje,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("📥 [NOTIFICACIONES] Mensaje recibido: {}", mensaje);

            // Simulación de envío de correo/SMS al cliente
            String cliente = (String) mensaje.getOrDefault("cliente", "Desconocido");
            log.info("📧 [NOTIFICACIONES] Enviando notificación al cliente: {}", cliente);

            // ACK manual: mensaje procesado exitosamente
            channel.basicAck(deliveryTag, false);
            log.info("✅ [NOTIFICACIONES] Notificación enviada y confirmada.");

        } catch (Exception e) {
            log.error("❌ [NOTIFICACIONES] Error enviando notificación: {}. Derivando a DLQ.", e.getMessage());
            // NACK sin requeue → DLQ
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
