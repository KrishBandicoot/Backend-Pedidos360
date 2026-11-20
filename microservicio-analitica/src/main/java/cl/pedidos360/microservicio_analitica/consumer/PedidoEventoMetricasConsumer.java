package cl.pedidos360.microservicio_analitica.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.pedidos360.microservicio_analitica.config.KafkaConstants;
import cl.pedidos360.microservicio_analitica.dto.MetricaClienteDto;
import cl.pedidos360.microservicio_analitica.dto.PedidoEventoDto;
import cl.pedidos360.microservicio_analitica.producer.MetricaProducer;
import cl.pedidos360.microservicio_analitica.service.MetricasService;

/**
 * Consumer group "analitica-metricas-group": con concurrency=3 las 3 particiones de
 * pedidos.eventos se reparten entre hilos (y entre instancias si se levantan mas).
 * Solo orquesta: la logica esta en MetricasService, la publicacion en MetricaProducer.
 */
@Component
public class PedidoEventoMetricasConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventoMetricasConsumer.class);

    private final MetricasService metricasService;
    private final MetricaProducer metricaProducer;

    public PedidoEventoMetricasConsumer(MetricasService metricasService, MetricaProducer metricaProducer) {
        this.metricasService = metricasService;
        this.metricaProducer = metricaProducer;
    }

    @KafkaListener(id = "analitica-metricas-listener",
            topics = KafkaConstants.TOPIC_PEDIDOS_EVENTOS,
            groupId = KafkaConstants.GROUP_METRICAS)
    public void onPedidoEvento(ConsumerRecord<String, PedidoEventoDto> record) {
        log.info("📥 [METRICAS] {}-{}@{} key={}", record.topic(), record.partition(), record.offset(), record.key());
        MetricaClienteDto metrica = metricasService.registrar(record.value());
        metricaProducer.publicar(metrica);
    }
}
