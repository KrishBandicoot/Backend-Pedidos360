package cl.pedidos360.microservicio_pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para la creación dinámica de bindings vía API REST.
 */
public class CreateBindingDto {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]+$",
             message = "El nombre de la cola solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String queueName;

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]+$",
             message = "El nombre del exchange solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String exchangeName;

    @NotBlank(message = "La routing key no puede estar vacía")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_#*]+$",
             message = "La routing key solo puede contener letras, números, puntos, guiones, guiones bajos, # y *")
    private String routingKey;

    public CreateBindingDto() {
    }

    public CreateBindingDto(String queueName, String exchangeName, String routingKey) {
        this.queueName = queueName;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
}
