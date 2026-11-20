package cl.pedidos360.microservicio_pedidos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import cl.pedidos360.microservicio_pedidos.dto.MetricaClienteDto;

class AnaliticaCacheServiceTest {

    private final AnaliticaCacheService service = new AnaliticaCacheService();

    @Test
    void actualizar_GuardaUltimaMetricaPorCliente() {
        Instant t = Instant.now();
        service.actualizar(new MetricaClienteDto("ACME", 1, 0, 1L, "Pendiente", t));
        service.actualizar(new MetricaClienteDto("ACME", 2, 0, 2L, "Pendiente", t.plusSeconds(5)));
        assertEquals(2, service.obtener("ACME").orElseThrow().pedidosCreados());
    }

    @Test
    void actualizar_IgnoraMetricaMasAntigua() {
        Instant t = Instant.now();
        service.actualizar(new MetricaClienteDto("ACME", 5, 0, 5L, "Pendiente", t));
        service.actualizar(new MetricaClienteDto("ACME", 1, 0, 1L, "Pendiente", t.minusSeconds(60)));
        assertEquals(5, service.obtener("ACME").orElseThrow().pedidosCreados());
    }

    @Test
    void actualizar_PayloadInvalido_LanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizar(new MetricaClienteDto(" ", 0, 0, null, null, Instant.now())));
    }
}
