package cl.pedidos360.microservicio_analitica.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import cl.pedidos360.microservicio_analitica.dto.MetricaClienteDto;
import cl.pedidos360.microservicio_analitica.dto.PedidoEventoDto;
import cl.pedidos360.microservicio_analitica.dto.TipoEventoPedido;

class MetricasServiceTest {

    private final MetricasService service = new MetricasService();

    private PedidoEventoDto evento(String id, TipoEventoPedido tipo, long pedidoId, String cliente) {
        return new PedidoEventoDto(id, tipo, pedidoId, cliente, "Pendiente", Instant.now());
    }

    @Test
    void registrar_CuentaCreadosYEliminadosPorCliente() {
        service.registrar(evento("e1", TipoEventoPedido.PEDIDO_CREADO, 1, "ACME"));
        service.registrar(evento("e2", TipoEventoPedido.PEDIDO_CREADO, 2, "ACME"));
        MetricaClienteDto m = service.registrar(evento("e3", TipoEventoPedido.PEDIDO_ELIMINADO, 1, "ACME"));

        assertEquals(2, m.pedidosCreados());
        assertEquals(1, m.pedidosEliminados());
        assertEquals(1L, m.ultimoPedidoId());
    }

    @Test
    void registrar_EventoRepetido_NoCuentaDosVeces() {
        service.registrar(evento("e1", TipoEventoPedido.PEDIDO_CREADO, 1, "ACME"));
        MetricaClienteDto m = service.registrar(evento("e1", TipoEventoPedido.PEDIDO_CREADO, 1, "ACME"));

        assertEquals(1, m.pedidosCreados());
    }

    @Test
    void registrar_ClientesDistintos_MetricasIndependientes() {
        service.registrar(evento("e1", TipoEventoPedido.PEDIDO_CREADO, 1, "ACME"));
        service.registrar(evento("e2", TipoEventoPedido.PEDIDO_CREADO, 2, "Globex"));

        assertEquals(1, service.obtener("ACME").orElseThrow().pedidosCreados());
        assertEquals(1, service.obtener("Globex").orElseThrow().pedidosCreados());
    }

    @Test
    void registrar_EventoInvalido_LanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.registrar(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(evento("e1", TipoEventoPedido.PEDIDO_CREADO, -5, "ACME")));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(evento("e2", TipoEventoPedido.PEDIDO_CREADO, 5, " ")));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(evento("e3", null, 5, "ACME")));
    }
}
