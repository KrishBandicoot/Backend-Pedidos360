package cl.pedidos360.microservicio_pedidos.dto;

import java.time.Instant;

/** Métrica por cliente que Analítica publica en pedidos.analitica y Pedidos consume. */
public record MetricaClienteDto(
        String cliente,
        long pedidosCreados,
        long pedidosEliminados,
        Long ultimoPedidoId,
        String ultimoEstado,
        Instant actualizadoEn) {
}
