package cl.pedidos360.microservicio_pedidos.service;

import cl.pedidos360.microservicio_pedidos.dto.QueueInfoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Servicio de administración de RabbitMQ.
 * <p>
 * Encapsula {@link RabbitAdmin} de Spring AMQP para gestionar colas,
 * exchanges y bindings en tiempo de ejecución a través del API REST.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    // ======================== COLAS ========================

    /**
     * Declara (crea) una cola duradera en el broker.
     */
    public void declareQueue(String name, boolean durable) {
        Queue queue = durable
                ? QueueBuilder.durable(name).build()
                : QueueBuilder.nonDurable(name).build();
        rabbitAdmin.declareQueue(queue);
        log.info("✅ Cola creada: {} (durable={})", name, durable);
    }

    /**
     * Elimina una cola del broker.
     *
     * @return true si la cola fue eliminada, false si no existía.
     */
    public boolean deleteQueue(String name) {
        boolean deleted = rabbitAdmin.deleteQueue(name);
        if (deleted) {
            log.info("🗑️ Cola eliminada: {}", name);
        } else {
            log.warn("⚠ Cola no encontrada para eliminar: {}", name);
        }
        return deleted;
    }

    /**
     * Obtiene información de una cola: cantidad de mensajes y consumidores.
     *
     * @return {@link QueueInfoDto} con el estado de la cola, o {@code null} si no existe.
     */
    public QueueInfoDto getQueueInfo(String name) {
        Properties props = rabbitAdmin.getQueueProperties(name);
        if (props == null) {
            return null;
        }

        int messageCount = props.get("QUEUE_MESSAGE_COUNT") != null ? 
                Integer.parseInt(props.get("QUEUE_MESSAGE_COUNT").toString()) : 0;
                
        int consumerCount = props.get("QUEUE_CONSUMER_COUNT") != null ? 
                Integer.parseInt(props.get("QUEUE_CONSUMER_COUNT").toString()) : 0;

        return new QueueInfoDto(name, messageCount, consumerCount);
    }

    // ======================== EXCHANGES ========================

    /**
     * Declara un exchange dinámicamente según su tipo (direct, topic, fanout, headers).
     */
    public void declareExchange(String name, String type, boolean durable) {
        switch (type.toLowerCase()) {
            case "direct" -> rabbitAdmin.declareExchange(new DirectExchange(name, durable, false));
            case "topic"  -> rabbitAdmin.declareExchange(new TopicExchange(name, durable, false));
            case "fanout" -> rabbitAdmin.declareExchange(new FanoutExchange(name, durable, false));
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + type);
        }
        log.info("✅ Exchange creado: {} (type={}, durable={})", name, type, durable);
    }

    // ======================== BINDINGS ========================

    /**
     * Crea un binding entre una cola y un exchange con la routing key proporcionada.
     * <p>
     * NOTA: usa Direct binding por defecto. Si se requiere Topic, se puede adaptar.
     */
    public void declareBinding(String queueName, String exchangeName, String routingKey) {
        Queue queue = new Queue(queueName);
        DirectExchange exchange = new DirectExchange(exchangeName);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        rabbitAdmin.declareBinding(binding);
        log.info("✅ Binding creado: {} → {} [key={}]", queueName, exchangeName, routingKey);
    }
}
