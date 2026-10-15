package cl.pedidos360.microservicio_pedidos.producer;

import cl.pedidos360.microservicio_pedidos.config.RabbitMQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Productor de eventos de Pedidos360.
 * <p>
 * Inyecta {@link RabbitTemplate} y publica mensajes hacia los exchanges
 * configurados, sin valores quemados (usa {@link RabbitMQConstants}).
 */
@Service
public class PedidoEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public PedidoEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica un evento de pedido creado hacia el Direct Exchange.
     * La cola {@code pedidos.procesamiento.queue} lo recibirá.
     */
    public void publicarPedidoCreado(Map<String, Object> payload) {
        log.info("▶ Publicando evento [pedido.creado] al Direct Exchange → {}", payload);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.DIRECT_EXCHANGE,
                RabbitMQConstants.ROUTING_KEY_PEDIDO_CREADO,
                payload
        );
    }

    /**
     * Publica un evento de facturación hacia el Direct Exchange.
     * La cola {@code facturacion.tickets.queue} lo recibirá.
     */
    public void publicarEventoFacturacion(Map<String, Object> payload) {
        log.info("▶ Publicando evento [pedido.factura] al Direct Exchange → {}", payload);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.DIRECT_EXCHANGE,
                RabbitMQConstants.ROUTING_KEY_PEDIDO_FACTURA,
                payload
        );
    }

    /**
     * Publica un evento de notificación hacia el Topic Exchange.
     * La cola {@code notificaciones.cliente.queue} lo recibirá.
     */
    public void publicarEventoNotificacion(Map<String, Object> payload) {
        log.info("▶ Publicando evento [pedido.notif] al Topic Exchange → {}", payload);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.TOPIC_EXCHANGE,
                RabbitMQConstants.ROUTING_KEY_PEDIDO_NOTIF,
                payload
        );
    }

    /**
     * Publica un evento simultáneamente a las 3 colas de negocio.
     * Útil para simular el flujo completo de un pedido.
     */
    public void publicarEventoCompleto(Map<String, Object> payload) {
        Map<String, Object> evento = Map.of(
                "pedidoId", payload.getOrDefault("pedidoId", 0),
                "cliente", payload.getOrDefault("cliente", "N/A"),
                "monto", payload.getOrDefault("monto", 0),
                "timestamp", LocalDateTime.now().toString()
        );

        publicarPedidoCreado(evento);
        publicarEventoFacturacion(evento);
        publicarEventoNotificacion(evento);

        log.info("✅ Evento completo publicado a las 3 colas de negocio.");
    }
}
