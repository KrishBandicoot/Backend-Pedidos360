package cl.pedidos360.microservicio_kafka_admin.dto;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateTopicRequest(
        @NotBlank(message = "El nombre del topico es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,249}$", message = "Nombre invalido: solo letras, numeros, '.', '_' y '-'")
        String name,
        @NotNull(message = "partitions es obligatorio")
        @Min(value = 1, message = "partitions debe ser >= 1")
        @Max(value = 50, message = "partitions debe ser <= 50")
        Integer partitions,
        @NotNull(message = "replicationFactor es obligatorio")
        @Min(value = 1, message = "replicationFactor debe ser >= 1")
        @Max(value = 5, message = "replicationFactor debe ser <= 5")
        Integer replicationFactor,
        Map<String, String> configs) {
}
