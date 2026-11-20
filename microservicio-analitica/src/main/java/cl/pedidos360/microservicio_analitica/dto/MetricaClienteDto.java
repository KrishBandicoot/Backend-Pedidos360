package cl.pedidos360.microservicio_analitica.dto;

import java.time.Instant;

/** Evento de salida publicado en pedidos.analitica (key = cliente). */
public record MetricaClienteDto(
        String cliente,
        long pedidosCreados,
        long pedidosEliminados,
        Long ultimoPedidoId,
        String ultimoEstado,
        Instant actualizadoEn) {
}
