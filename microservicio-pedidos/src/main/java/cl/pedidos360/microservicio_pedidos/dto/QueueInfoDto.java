package cl.pedidos360.microservicio_pedidos.dto;

/**
 * DTO de respuesta con información de estado de una cola.
 */
public class QueueInfoDto {

    private String name;
    private int messageCount;
    private int consumerCount;

    public QueueInfoDto() {
    }

    public QueueInfoDto(String name, int messageCount, int consumerCount) {
        this.name = name;
        this.messageCount = messageCount;
        this.consumerCount = consumerCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int getConsumerCount() {
        return consumerCount;
    }

    public void setConsumerCount(int consumerCount) {
        this.consumerCount = consumerCount;
    }
}
