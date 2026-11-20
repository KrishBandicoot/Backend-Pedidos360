package cl.pedidos360.microservicio_kafka_admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record IncreasePartitionsRequest(
        @NotNull(message = "totalPartitions es obligatorio")
        @Min(value = 2, message = "totalPartitions debe ser >= 2")
        @Max(value = 50, message = "totalPartitions debe ser <= 50")
        Integer totalPartitions) {
}
