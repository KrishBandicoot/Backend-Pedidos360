package cl.pedidos360.microservicio_analitica.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cl.pedidos360.microservicio_analitica.dto.MetricaClienteDto;
import cl.pedidos360.microservicio_analitica.dto.PedidoEventoDto;

/**
 * Logica de dominio de analitica. No depende de Kafka.
 * - Valida el evento: si es invalido lanza IllegalArgumentException (va directo al DLT).
 * - Es idempotente por eventId: Kafka entrega "al menos una vez", asi que una
 *   reentrega no debe contar dos veces el mismo pedido.
 * Estado en memoria: se pierde al reiniciar el servicio (suficiente para esta evaluacion).
 */
@Service
public class MetricasService {

    private static final Logger log = LoggerFactory.getLogger(MetricasService.class);
    private static final int MAX_EVENTOS_RECORDADOS = 10_000;

    private final Map<String, MetricaClienteDto> metricas = new ConcurrentHashMap<>();

    private final Set<String> eventosProcesados = Collections.newSetFromMap(
            Collections.synchronizedMap(new LinkedHashMap<String, Boolean>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_EVENTOS_RECORDADOS;
                }
            }));

    public MetricaClienteDto registrar(PedidoEventoDto evento) {
        validar(evento);

        if (!eventosProcesados.add(evento.eventId())) {
            log.debug("Evento {} ya procesado; se ignora (idempotencia)", evento.eventId());
            return metricas.getOrDefault(evento.cliente(),
                    new MetricaClienteDto(evento.cliente(), 0, 0, evento.pedidoId(), evento.estado(), Instant.now()));
        }

        return metricas.compute(evento.cliente(), (cliente, actual) -> {
            long creados = actual == null ? 0 : actual.pedidosCreados();
            long eliminados = actual == null ? 0 : actual.pedidosEliminados();
            switch (evento.tipo()) {
                case PEDIDO_CREADO -> creados++;
                case PEDIDO_ELIMINADO -> eliminados++;
            }
            return new MetricaClienteDto(cliente, creados, eliminados,
                    evento.pedidoId(), evento.estado(), Instant.now());
        });
    }

    public Collection<MetricaClienteDto> listar() {
        return metricas.values();
    }

    public Optional<MetricaClienteDto> obtener(String cliente) {
        return Optional.ofNullable(metricas.get(cliente));
    }

    private void validar(PedidoEventoDto e) {
        if (e == null) {
            throw new IllegalArgumentException("Evento nulo");
        }
        if (e.eventId() == null || e.eventId().isBlank()) {
            throw new IllegalArgumentException("Evento sin eventId");
        }
        if (e.tipo() == null) {
            throw new IllegalArgumentException("Evento sin tipo");
        }
        if (e.pedidoId() == null || e.pedidoId() <= 0) {
            throw new IllegalArgumentException("pedidoId invalido: " + e.pedidoId());
        }
        if (e.cliente() == null || e.cliente().isBlank()) {
            throw new IllegalArgumentException("Evento sin cliente");
        }
    }
}
