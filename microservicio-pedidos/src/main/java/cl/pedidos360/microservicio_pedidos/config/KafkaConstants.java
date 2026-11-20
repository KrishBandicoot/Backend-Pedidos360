package cl.pedidos360.microservicio_pedidos.config;

/**
 * Constantes Kafka de Pedidos360 (equivalente a RabbitMQConstants).
 * Los nombres de tópicos deben coincidir con los que crea el microservicio administrador.
 */
public final class KafkaConstants {

    private KafkaConstants() {
    }

    // Tópicos principales
    public static final String TOPIC_PEDIDOS_EVENTOS   = "pedidos.eventos";
    public static final String TOPIC_PEDIDOS_ANALITICA = "pedidos.analitica";

    // Dead Letter Topics: <tópico>.DLT
    public static final String DLT_SUFFIX = ".DLT";

    // Consumer groups de este microservicio
    public static final String GROUP_PEDIDOS_METRICAS = "pedidos-metricas-group";
}
