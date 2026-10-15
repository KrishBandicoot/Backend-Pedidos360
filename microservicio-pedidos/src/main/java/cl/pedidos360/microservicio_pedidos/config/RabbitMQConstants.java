package cl.pedidos360.microservicio_pedidos.config;

/**
 * Clase centralizadora de constantes AMQP para el proyecto Pedidos360.
 * Todos los nombres de colas, exchanges, routing keys, DLQ y DLX se definen
 * aquí como {@code public static final String} para evitar valores quemados
 * en la lógica de negocio.
 */
public final class RabbitMQConstants {

    private RabbitMQConstants() {
        // Clase de utilidad — no instanciar
    }

    // ======================== EXCHANGES PRINCIPALES ========================
    public static final String DIRECT_EXCHANGE = "pedidos.direct.exchange";
    public static final String TOPIC_EXCHANGE  = "pedidos.topic.exchange";

    // ======================== COLAS DE NEGOCIO ========================
    public static final String PEDIDOS_PROCESAMIENTO_QUEUE   = "pedidos.procesamiento.queue";
    public static final String NOTIFICACIONES_CLIENTE_QUEUE  = "notificaciones.cliente.queue";
    public static final String FACTURACION_TICKETS_QUEUE     = "facturacion.tickets.queue";

    // ======================== ROUTING KEYS PRINCIPALES ========================
    public static final String ROUTING_KEY_PEDIDO_CREADO     = "pedido.creado";
    public static final String ROUTING_KEY_PEDIDO_NOTIF      = "pedido.notif";
    public static final String ROUTING_KEY_PEDIDO_FACTURA    = "pedido.factura";

    // ======================== DEAD LETTER EXCHANGES (DLX) ========================
    public static final String PEDIDOS_DLX        = "pedidos.direct.dlx";
    public static final String FACTURACION_DLX    = "facturacion.direct.dlx";
    public static final String NOTIFICACIONES_DLX = "notificaciones.topic.dlx";

    // ======================== DEAD LETTER QUEUES (DLQ) ========================
    public static final String PEDIDOS_PROCESAMIENTO_DLQ   = "pedidos.procesamiento.dlq";
    public static final String FACTURACION_TICKETS_DLQ     = "facturacion.tickets.dlq";
    public static final String NOTIFICACIONES_CLIENTE_DLQ  = "notificaciones.cliente.dlq";

    // ======================== DEAD LETTER ROUTING KEYS ========================
    public static final String DLK_PEDIDOS        = "pedidos.procesamiento.deadletter";
    public static final String DLK_FACTURACION    = "facturacion.tickets.deadletter";
    public static final String DLK_NOTIFICACIONES = "notificaciones.cliente.deadletter";
}
