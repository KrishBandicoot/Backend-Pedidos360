package cl.pedidos360.microservicio_kafka_admin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record UpdateTopicConfigRequest(
        @NotEmpty(message = "Debes indicar al menos una configuracion")
        Map<String, String> configs) {
}
