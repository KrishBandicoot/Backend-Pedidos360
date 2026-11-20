package cl.pedidos360.microservicio_analitica.config;

public final class KafkaConstants {

    private KafkaConstants() {
    }

    public static final String TOPIC_PEDIDOS_EVENTOS   = "pedidos.eventos";
    public static final String TOPIC_PEDIDOS_ANALITICA = "pedidos.analitica";
    public static final String DLT_SUFFIX = ".DLT";

    /** Grupo que calcula metricas: sus consumidores se reparten las particiones. */
    public static final String GROUP_METRICAS  = "analitica-metricas-group";
    /** Grupo independiente: recibe TODOS los eventos otra vez (fan-out) para auditoria. */
    public static final String GROUP_AUDITORIA = "analitica-auditoria-group";
}
