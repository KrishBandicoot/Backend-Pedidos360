package cl.pedidos360.microservicio_pedidos.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.pedidos360.microservicio_pedidos.dto.TipoEventoPedido;
import cl.pedidos360.microservicio_pedidos.exception.ResourceNotFoundException;
import cl.pedidos360.microservicio_pedidos.model.Pedido;
import cl.pedidos360.microservicio_pedidos.producer.PedidoKafkaProducer;
import cl.pedidos360.microservicio_pedidos.service.PedidoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final PedidoKafkaProducer kafkaProducer;

    public PedidoController(PedidoService pedidoService, PedidoKafkaProducer kafkaProducer) {
        this.pedidoService = pedidoService;
        this.kafkaProducer = kafkaProducer;
    }

    /**
     * Consulta general de pedidos.
     * Permitido para usuarios con rol USER, rol ADMIN o scope delegado Pedidos.Read.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER') or hasAuthority('SCOPE_Pedidos.Read')")
    public ResponseEntity<List<Pedido>> listarPedidos() {
        return ResponseEntity.ok(pedidoService.obtenerTodos());
    }

    /**
     * Consulta de pedido por ID.
     * Permitido para usuarios con rol USER, rol ADMIN o scope delegado Pedidos.Read.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER') or hasAuthority('SCOPE_Pedidos.Read')")
    public ResponseEntity<Pedido> obtenerPedido(@PathVariable Long id) {
        return pedidoService.obtenerPorId(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));
    }

    /**
     * Creación de nuevo pedido con validación de entrada.
     * Permitido para rol ADMIN o scope delegado Pedidos.Write.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_Pedidos.Write')")
    public ResponseEntity<Pedido> crearPedido(@Valid @RequestBody Pedido pedido) {
        Pedido creado = pedidoService.crearPedido(pedido);
        // Se publica DESPUÉS de persistir; si Kafka falla, el pedido igual queda creado.
        kafkaProducer.publicar(TipoEventoPedido.PEDIDO_CREADO, creado);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    /**
     * Eliminación de pedido.
     * Restringido estrictamente a usuarios con rol ADMIN (Zero Trust RBAC).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarPedido(@PathVariable Long id) {
        // Se lee antes de borrar para poder informar cliente y estado en el evento.
        Optional<Pedido> existente = pedidoService.obtenerPorId(id);
        pedidoService.eliminarPedido(id);
        existente.ifPresent(p -> kafkaProducer.publicar(TipoEventoPedido.PEDIDO_ELIMINADO, p));
        return ResponseEntity.noContent().build();
    }
}
