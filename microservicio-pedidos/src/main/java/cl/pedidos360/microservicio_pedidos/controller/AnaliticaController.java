package cl.pedidos360.microservicio_pedidos.controller;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.pedidos360.microservicio_pedidos.dto.MetricaClienteDto;
import cl.pedidos360.microservicio_pedidos.exception.ResourceNotFoundException;
import cl.pedidos360.microservicio_pedidos.service.AnaliticaCacheService;

/** Expone las métricas por cliente recibidas vía Kafka. */
@RestController
@RequestMapping("/api/analitica")
public class AnaliticaController {

    private final AnaliticaCacheService cacheService;

    public AnaliticaController(AnaliticaCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/clientes")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Collection<MetricaClienteDto>> listar() {
        return ResponseEntity.ok(cacheService.listar());
    }

    @GetMapping("/clientes/{cliente}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<MetricaClienteDto> obtener(@PathVariable String cliente) {
        return cacheService.obtener(cliente)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Sin métricas para el cliente: " + cliente));
    }
}
