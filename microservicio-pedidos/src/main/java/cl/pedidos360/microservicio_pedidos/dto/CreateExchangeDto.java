package cl.pedidos360.microservicio_pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para la creación dinámica de exchanges vía API REST.
 */
public class CreateExchangeDto {

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]+$",
             message = "El nombre del exchange solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String name;

    @NotBlank(message = "El tipo de exchange no puede estar vacío")
    @Pattern(regexp = "^(direct|topic|fanout|headers)$",
             message = "El tipo de exchange debe ser: direct, topic, fanout o headers")
    private String type;

    private boolean durable = true;

    public CreateExchangeDto() {
    }

    public CreateExchangeDto(String name, String type, boolean durable) {
        this.name = name;
        this.type = type;
        this.durable = durable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }
}
