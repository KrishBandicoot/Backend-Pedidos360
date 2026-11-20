package cl.pedidos360.microservicio_analitica.producer;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import cl.pedidos360.microservicio_analitica.config.KafkaConstants;
import cl.pedidos360.microservicio_analitica.dto.MetricaClienteDto;

/**
 * Publica metricas en pedidos.analitica con key = cliente
 * (el topico es compactado: queda la ultima metrica de cada cliente).
 * Espera la confirmacion del broker: si falla, la excepcion sube al listener
 * y el error handler reintenta / envia al DLT.
 */
@Service
public class MetricaProducer {

    private static final Logger log = LoggerFactory.getLogger(MetricaProducer.class);

    private final KafkaTemplate<String, MetricaClienteDto> kafkaTemplate;

    public MetricaProducer(KafkaTemplate<String, MetricaClienteDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(MetricaClienteDto metrica) {
        try {
            SendResult<String, MetricaClienteDto> r = kafkaTemplate
                    .send(KafkaConstants.TOPIC_PEDIDOS_ANALITICA, metrica.cliente(), metrica)
                    .get(10, TimeUnit.SECONDS);
            log.info("▶ [KAFKA] Metrica cliente={} → {}-{}@{}", metrica.cliente(),
                    r.getRecordMetadata().topic(), r.getRecordMetadata().partition(), r.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Publicacion interrumpida", e);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo publicar la metrica de " + metrica.cliente(), e);
        }
    }
}
