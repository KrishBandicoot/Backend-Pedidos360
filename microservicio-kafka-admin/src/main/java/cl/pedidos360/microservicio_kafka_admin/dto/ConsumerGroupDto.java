package cl.pedidos360.microservicio_kafka_admin.dto;

import java.util.List;

public record ConsumerGroupDto(String groupId, String state, Integer coordinatorId,
                               List<MemberDto> members, List<OffsetLagDto> offsets, long totalLag) {

    public record MemberDto(String memberId, String clientId, String host, List<String> assignedPartitions) {
    }

    /** lag = logEndOffset - currentOffset: mensajes pendientes de procesar por el grupo. */
    public record OffsetLagDto(String topic, int partition, long currentOffset, long logEndOffset, long lag) {
    }
}
