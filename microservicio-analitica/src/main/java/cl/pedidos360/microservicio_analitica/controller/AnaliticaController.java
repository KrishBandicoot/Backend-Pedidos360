package cl.pedidos360.microservicio_analitica.controller;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.pedidos360.microservicio_analitica.dto.MetricaClienteDto;
import cl.pedidos360.microservicio_analitica.service.MetricasService;

@RestController
@RequestMapping("/api/analitica")
public class AnaliticaController {

    private final MetricasService metricasService;

    public AnaliticaController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping("/clientes")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Collection<MetricaClienteDto>> listar() {
        return ResponseEntity.ok(metricasService.listar());
    }

    @GetMapping("/clientes/{cliente}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<MetricaClienteDto> obtener(@PathVariable String cliente) {
        return metricasService.obtener(cliente)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
