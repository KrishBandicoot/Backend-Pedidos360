package cl.pedidos360.microservicio_pedidos.producer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import cl.pedidos360.microservicio_pedidos.config.KafkaConstants;
import cl.pedidos360.microservicio_pedidos.dto.PedidoEventoDto;
import cl.pedidos360.microservicio_pedidos.dto.TipoEventoPedido;
import cl.pedidos360.microservicio_pedidos.model.Pedido;

class PedidoKafkaProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, PedidoEventoDto> template = mock(KafkaTemplate.class);
    private final PedidoKafkaProducer producer = new PedidoKafkaProducer(template);

    @Test
    @DisplayName("Publica en pedidos.eventos usando el id del pedido como key")
    void publicar_UsaTopicYKeyCorrectos() {
        when(template.send(eq(KafkaConstants.TOPIC_PEDIDOS_EVENTOS), eq("7"), any(PedidoEventoDto.class)))
                .thenReturn(new CompletableFuture<>());

        producer.publicar(TipoEventoPedido.PEDIDO_CREADO, new Pedido(7L, "Cliente Uno", "Pendiente"));

        ArgumentCaptor<PedidoEventoDto> captor = ArgumentCaptor.forClass(PedidoEventoDto.class);
        verify(template).send(eq(KafkaConstants.TOPIC_PEDIDOS_EVENTOS), eq("7"), captor.capture());
        PedidoEventoDto evento = captor.getValue();
        assertEquals(TipoEventoPedido.PEDIDO_CREADO, evento.tipo());
        assertEquals("Cliente Uno", evento.cliente());
        assertNotNull(evento.eventId());
    }

    @Test
    @DisplayName("Si Kafka falla, la API no se rompe")
    void publicar_ConKafkaCaido_NoLanzaExcepcion() {
        when(template.send(any(String.class), any(String.class), any(PedidoEventoDto.class)))
                .thenThrow(new IllegalStateException("broker caido"));

        assertDoesNotThrow(() ->
                producer.publicar(TipoEventoPedido.PEDIDO_ELIMINADO, new Pedido(8L, "Cliente Dos", "Completado")));
    }
}
