package cl.pedidos360.microservicio_pedidos.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Manejo de errores de consumidores Kafka.
 * <p>
 * Un mensaje que falla se reintenta 3 veces con backoff exponencial (1s, 2s, 4s).
 * Si sigue fallando (o el error no es reintentable, como un payload inválido),
 * se publica en <tópico>.DLT con el stacktrace en headers y se registra en logs.
 * Spring Boot toma automáticamente este bean como CommonErrorHandler de los listeners.
 */
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(ProducerFactory<?, ?> producerFactory) {
        Map<String, Object> base = new HashMap<>(producerFactory.getConfigurationProperties());

        // Mensajes que no se pudieron deserializar viajan como bytes crudos...
        Map<String, Object> bytesProps = new HashMap<>(base);
        bytesProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        bytesProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        // ...y los que fallaron en la lógica de negocio, como JSON.
        Map<String, Object> jsonProps = new HashMap<>(base);
        jsonProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        jsonProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        KafkaTemplate<Object, Object> bytesTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(bytesProps));
        KafkaTemplate<Object, Object> jsonTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(jsonProps));

        Map<Class<?>, KafkaOperations<? extends Object, ? extends Object>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, jsonTemplate); // el más genérico va al final

        // Partición -1: el productor elige; el DLT no necesita las mismas particiones que el origen
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(templates,
                (ConsumerRecord<?, ?> record, Exception ex) ->
                        new TopicPartition(record.topic() + KafkaConstants.DLT_SUFFIX, -1));

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(8000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // Un payload inválido no mejora reintentando: directo al DLT
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        handler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int attempt) {
                log.warn("⚠ [KAFKA] Fallo procesando {}-{}@{} (intento {}): {}",
                        record.topic(), record.partition(), record.offset(), attempt, ex.getMessage());
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                log.error("☠ [KAFKA] Mensaje {}-{}@{} enviado a {}{}. Causa: {}",
                        record.topic(), record.partition(), record.offset(),
                        record.topic(), KafkaConstants.DLT_SUFFIX, ex.getMessage());
            }
        });
        return handler;
    }
}
