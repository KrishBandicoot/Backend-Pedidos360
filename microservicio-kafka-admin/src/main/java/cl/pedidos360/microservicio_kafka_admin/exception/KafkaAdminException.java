package cl.pedidos360.microservicio_kafka_admin.exception;

import org.springframework.http.HttpStatus;

/** Error de administracion de Kafka con el HTTP status que corresponde devolver. */
public class KafkaAdminException extends RuntimeException {

    private final HttpStatus status;

    public KafkaAdminException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public KafkaAdminException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
