package cl.pedidos360.microservicio_pedidos.dto;

import java.time.Instant;

/** Evento que Pedidos publica en el tópico pedidos.eventos. */
public record PedidoEventoDto(
        String eventId,
        TipoEventoPedido tipo,
        Long pedidoId,
        String cliente,
        String estado,
        Instant fechaEvento) {
}
