package cl.pedidos360.microservicio_kafka_admin.dto;

import java.util.List;

public record ClusterInfoDto(String clusterId, Integer controllerId, List<BrokerDto> brokers) {

    public record BrokerDto(int id, String host, int port) {
    }
}
