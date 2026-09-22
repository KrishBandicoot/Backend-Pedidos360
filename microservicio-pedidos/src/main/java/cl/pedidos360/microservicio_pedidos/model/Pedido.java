package cl.pedidos360.microservicio_pedidos.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del cliente es obligatorio y no puede estar vacío.")
    @Size(min = 3, max = 100, message = "El nombre del cliente debe tener entre 3 y 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String cliente;

    @NotBlank(message = "El estado del pedido es obligatorio.")
    @Pattern(
        regexp = "^(Pendiente|En Preparacion|Completado)$",
        message = "El estado debe ser exactamente uno de los siguientes: 'Pendiente', 'En Preparacion' o 'Completado'."
    )
    @Column(nullable = false, length = 30)
    private String estado;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    public Pedido() {
    }

    public Pedido(Long id, String cliente, String estado) {
        this.id = id;
        this.cliente = cliente;
        this.estado = estado;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
