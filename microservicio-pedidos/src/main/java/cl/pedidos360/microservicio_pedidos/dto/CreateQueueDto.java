package cl.pedidos360.microservicio_pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para la creación dinámica de colas vía API REST.
 * <p>
 * Aplica validaciones Jakarta:
 * <ul>
 *   <li>{@code @NotBlank}: no permite nombres vacíos o solo espacios.</li>
 *   <li>{@code @Pattern}: solo permite caracteres alfanuméricos, puntos, guiones y guiones bajos.</li>
 * </ul>
 */
public class CreateQueueDto {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]+$",
             message = "El nombre de la cola solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String name;

    private boolean durable = true;

    public CreateQueueDto() {
    }

    public CreateQueueDto(String name, boolean durable) {
        this.name = name;
        this.durable = durable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }
}
