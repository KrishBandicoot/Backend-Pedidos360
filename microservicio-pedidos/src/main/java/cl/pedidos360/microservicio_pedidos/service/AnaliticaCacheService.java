package cl.pedidos360.microservicio_pedidos.service;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

import cl.pedidos360.microservicio_pedidos.dto.MetricaClienteDto;

/**
 * Lógica de dominio: guarda la última métrica por cliente recibida desde Analítica.
 * No conoce Kafka; el consumidor solo le delega el dato.
 */
@Service
public class AnaliticaCacheService {

    private final ConcurrentMap<String, MetricaClienteDto> metricas = new ConcurrentHashMap<>();

    public void actualizar(MetricaClienteDto metrica) {
        if (metrica == null || metrica.cliente() == null || metrica.cliente().isBlank()) {
            throw new IllegalArgumentException("Métrica inválida: falta el cliente");
        }
        // Solo reemplaza si es más nueva (protege ante reentregas fuera de orden)
        metricas.merge(metrica.cliente(), metrica, (actual, nueva) ->
                actual.actualizadoEn() != null && nueva.actualizadoEn() != null
                        && nueva.actualizadoEn().isBefore(actual.actualizadoEn()) ? actual : nueva);
    }

    public Collection<MetricaClienteDto> listar() {
        return metricas.values();
    }

    public Optional<MetricaClienteDto> obtener(String cliente) {
        return Optional.ofNullable(metricas.get(cliente));
    }
}
