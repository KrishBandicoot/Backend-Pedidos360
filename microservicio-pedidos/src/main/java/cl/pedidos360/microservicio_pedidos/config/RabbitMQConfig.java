package cl.pedidos360.microservicio_pedidos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static cl.pedidos360.microservicio_pedidos.config.RabbitMQConstants.*;

/**
 * Configuración central de la topología AMQP para Pedidos360.
 * <p>
 * Declara mediante @Bean:
 * <ul>
 *   <li>1 Direct Exchange y 1 Topic Exchange principales</li>
 *   <li>3 Colas de negocio con argumentos x-dead-letter-exchange y x-dead-letter-routing-key</li>
 *   <li>3 Dead Letter Exchanges (DLX)</li>
 *   <li>3 Dead Letter Queues (DLQ) independientes</li>
 *   <li>Bindings correspondientes (principales y DLQ)</li>
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

    // ======================== MESSAGE CONVERTER ========================

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter);
        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    // ======================== EXCHANGES PRINCIPALES ========================

    @Bean
    public DirectExchange pedidosDirectExchange() {
        return new DirectExchange(DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange pedidosTopicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    // ======================== DEAD LETTER EXCHANGES (DLX) ========================

    @Bean
    public DirectExchange pedidosDlx() {
        return new DirectExchange(PEDIDOS_DLX, true, false);
    }

    @Bean
    public DirectExchange facturacionDlx() {
        return new DirectExchange(FACTURACION_DLX, true, false);
    }

    @Bean
    public DirectExchange notificacionesDlx() {
        return new DirectExchange(NOTIFICACIONES_DLX, true, false);
    }

    // ======================== COLAS DE NEGOCIO (CON ARGS DLX / DLK) ========================

    @Bean
    public Queue pedidosProcesamientoQueue() {
        return QueueBuilder.durable(PEDIDOS_PROCESAMIENTO_QUEUE)
                .withArgument("x-dead-letter-exchange", PEDIDOS_DLX)
                .withArgument("x-dead-letter-routing-key", DLK_PEDIDOS)
                .build();
    }

    @Bean
    public Queue notificacionesClienteQueue() {
        return QueueBuilder.durable(NOTIFICACIONES_CLIENTE_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICACIONES_DLX)
                .withArgument("x-dead-letter-routing-key", DLK_NOTIFICACIONES)
                .build();
    }

    @Bean
    public Queue facturacionTicketsQueue() {
        return QueueBuilder.durable(FACTURACION_TICKETS_QUEUE)
                .withArgument("x-dead-letter-exchange", FACTURACION_DLX)
                .withArgument("x-dead-letter-routing-key", DLK_FACTURACION)
                .build();
    }

    // ======================== DEAD LETTER QUEUES (DLQ) ========================

    @Bean
    public Queue pedidosProcesamientoDlq() {
        return QueueBuilder.durable(PEDIDOS_PROCESAMIENTO_DLQ).build();
    }

    @Bean
    public Queue facturacionTicketsDlq() {
        return QueueBuilder.durable(FACTURACION_TICKETS_DLQ).build();
    }

    @Bean
    public Queue notificacionesClienteDlq() {
        return QueueBuilder.durable(NOTIFICACIONES_CLIENTE_DLQ).build();
    }

    // ======================== BINDINGS — COLAS PRINCIPALES A EXCHANGES ========================

    @Bean
    public Binding bindingPedidosProcesamiento(Queue pedidosProcesamientoQueue,
                                                DirectExchange pedidosDirectExchange) {
        return BindingBuilder.bind(pedidosProcesamientoQueue)
                .to(pedidosDirectExchange)
                .with(ROUTING_KEY_PEDIDO_CREADO);
    }

    @Bean
    public Binding bindingFacturacionTickets(Queue facturacionTicketsQueue,
                                              DirectExchange pedidosDirectExchange) {
        return BindingBuilder.bind(facturacionTicketsQueue)
                .to(pedidosDirectExchange)
                .with(ROUTING_KEY_PEDIDO_FACTURA);
    }

    @Bean
    public Binding bindingNotificacionesCliente(Queue notificacionesClienteQueue,
                                                 TopicExchange pedidosTopicExchange) {
        return BindingBuilder.bind(notificacionesClienteQueue)
                .to(pedidosTopicExchange)
                .with(ROUTING_KEY_PEDIDO_NOTIF);
    }

    // ======================== BINDINGS — DLQ A DLX ========================

    @Bean
    public Binding bindingPedidosDlq(Queue pedidosProcesamientoDlq,
                                      DirectExchange pedidosDlx) {
        return BindingBuilder.bind(pedidosProcesamientoDlq)
                .to(pedidosDlx)
                .with(DLK_PEDIDOS);
    }

    @Bean
    public Binding bindingFacturacionDlq(Queue facturacionTicketsDlq,
                                          DirectExchange facturacionDlx) {
        return BindingBuilder.bind(facturacionTicketsDlq)
                .to(facturacionDlx)
                .with(DLK_FACTURACION);
    }

    @Bean
    public Binding bindingNotificacionesDlq(Queue notificacionesClienteDlq,
                                              DirectExchange notificacionesDlx) {
        return BindingBuilder.bind(notificacionesClienteDlq)
                .to(notificacionesDlx)
                .with(DLK_NOTIFICACIONES);
    }
}
