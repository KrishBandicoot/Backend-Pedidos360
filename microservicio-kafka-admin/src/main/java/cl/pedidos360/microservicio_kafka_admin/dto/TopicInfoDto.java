package cl.pedidos360.microservicio_kafka_admin.dto;

import java.util.List;
import java.util.Map;

public record TopicInfoDto(String name, boolean internal, int partitionCount, int replicationFactor,
                           List<PartitionDto> partitions, Map<String, String> configs) {

    /** leader = -1 si la particion no tiene lider; isr = replicas en sincronia. */
    public record PartitionDto(int partition, int leader, List<Integer> replicas, List<Integer> isr) {
    }
}
