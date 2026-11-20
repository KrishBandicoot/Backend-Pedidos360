package cl.pedidos360.microservicio_analitica.dto;

import java.time.Instant;

/** Evento de entrada (mismo contrato JSON que publica microservicio-pedidos). */
public record PedidoEventoDto(
        String eventId,
        TipoEventoPedido tipo,
        Long pedidoId,
        String cliente,
        String estado,
        Instant fechaEvento) {
}
