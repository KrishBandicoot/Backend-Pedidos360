package cl.pedidos360.microservicio_pedidos.controller;

import cl.pedidos360.microservicio_pedidos.dto.CreateBindingDto;
import cl.pedidos360.microservicio_pedidos.dto.CreateExchangeDto;
import cl.pedidos360.microservicio_pedidos.dto.CreateQueueDto;
import cl.pedidos360.microservicio_pedidos.dto.QueueInfoDto;
import cl.pedidos360.microservicio_pedidos.producer.PedidoEventProducer;
import cl.pedidos360.microservicio_pedidos.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controlador REST de administración de RabbitMQ.
 * <p>
 * Expone endpoints bajo {@code /api/rabbitmq/admin} para gestionar
 * colas, exchanges y bindings en tiempo de ejecución, además de
 * endpoints de prueba para simular flujos exitosos y fallos a DLQ.
 */
@RestController
@RequestMapping("/api/rabbitmq/admin")
public class RabbitAdminController {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminController.class);

    private final RabbitAdminService rabbitAdminService;
    private final PedidoEventProducer pedidoEventProducer;

    public RabbitAdminController(RabbitAdminService rabbitAdminService,
                                  PedidoEventProducer pedidoEventProducer) {
        this.rabbitAdminService = rabbitAdminService;
        this.pedidoEventProducer = pedidoEventProducer;
    }

    // ======================== COLAS ========================

    /**
     * POST /api/rabbitmq/admin/queues
     * Crea una cola dinámica en el broker.
     */
    @PostMapping("/queues")
    public ResponseEntity<Map<String, String>> crearCola(@Valid @RequestBody CreateQueueDto dto) {
        rabbitAdminService.declareQueue(dto.getName(), dto.isDurable());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Cola creada exitosamente",
                        "queue", dto.getName()
                ));
    }

    /**
     * GET /api/rabbitmq/admin/queues/{name}
     * Consulta información de una cola (mensajes, consumidores).
     */
    @GetMapping("/queues/{name}")
    public ResponseEntity<?> obtenerInfoCola(@PathVariable String name) {
        QueueInfoDto info = rabbitAdminService.getQueueInfo(name);
        if (info == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Cola no encontrada: " + name));
        }
        return ResponseEntity.ok(info);
    }

    /**
     * DELETE /api/rabbitmq/admin/queues/{name}
     * Elimina una cola del broker.
     */
    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Map<String, String>> eliminarCola(@PathVariable String name) {
        boolean deleted = rabbitAdminService.deleteQueue(name);
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "message", "Cola eliminada exitosamente",
                    "queue", name
            ));
        }
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Cola no encontrada: " + name));
    }

    // ======================== EXCHANGES ========================

    /**
     * POST /api/rabbitmq/admin/exchanges
     * Declara un exchange dinámico (direct, topic, fanout, headers).
     */
    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> crearExchange(@Valid @RequestBody CreateExchangeDto dto) {
        rabbitAdminService.declareExchange(dto.getName(), dto.getType(), dto.isDurable());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Exchange creado exitosamente",
                        "exchange", dto.getName(),
                        "type", dto.getType()
                ));
    }

    // ======================== BINDINGS ========================

    /**
     * POST /api/rabbitmq/admin/bindings
     * Crea un binding entre una cola y un exchange con routing key.
     */
    @PostMapping("/bindings")
    public ResponseEntity<Map<String, String>> crearBinding(@Valid @RequestBody CreateBindingDto dto) {
        rabbitAdminService.declareBinding(dto.getQueueName(), dto.getExchangeName(), dto.getRoutingKey());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Binding creado exitosamente",
                        "queue", dto.getQueueName(),
                        "exchange", dto.getExchangeName(),
                        "routingKey", dto.getRoutingKey()
                ));
    }

    // ======================== ENDPOINTS DE PRUEBA ========================

    /**
     * POST /api/rabbitmq/admin/test/simular-exito
     * Publica un evento válido que impacta las 3 colas de negocio simultáneamente.
     */
    @PostMapping("/test/simular-exito")
    public ResponseEntity<Map<String, String>> simularFlujoExitoso() {
        Map<String, Object> payload = Map.of(
                "pedidoId", 1001,
                "cliente", "Kevin Pedidos360",
                "monto", 59990,
                "descripcion", "Pedido de prueba exitoso",
                "timestamp", LocalDateTime.now().toString()
        );

        pedidoEventProducer.publicarEventoCompleto(payload);

        log.info("🧪 [TEST] Flujo exitoso simulado → 3 colas impactadas.");
        return ResponseEntity.ok(Map.of(
                "message", "Evento publicado exitosamente a las 3 colas de negocio",
                "status", "SUCCESS"
        ));
    }

    /**
     * POST /api/rabbitmq/admin/test/simular-fallo-dlq
     * Envía un payload corrupto (monto <= 0) para forzar el flujo Dead Letter Queue.
     */
    @PostMapping("/test/simular-fallo-dlq")
    public ResponseEntity<Map<String, String>> simularFalloDlq() {
        Map<String, Object> payloadCorrupto = Map.of(
                "pedidoId", -999,
                "cliente", "PAYLOAD_CORRUPTO",
                "monto", -1,
                "descripcion", "Pedido intencional con datos inválidos para probar DLQ",
                "timestamp", LocalDateTime.now().toString()
        );

        pedidoEventProducer.publicarPedidoCreado(payloadCorrupto);

        log.warn("🧪 [TEST] Fallo DLQ simulado → Payload corrupto enviado a pedidos.procesamiento.queue.");
        return ResponseEntity.ok(Map.of(
                "message", "Payload corrupto enviado. Verificar consola para confirmar entrada en DLQ.",
                "status", "DLQ_TEST_TRIGGERED"
        ));
    }
}
