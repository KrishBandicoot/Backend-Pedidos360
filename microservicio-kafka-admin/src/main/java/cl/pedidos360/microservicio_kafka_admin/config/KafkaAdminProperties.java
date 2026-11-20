package cl.pedidos360.microservicio_kafka_admin.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Topicos gestionados, leidos de application.yml (prefijo kafka-admin). */
@Component
@ConfigurationProperties(prefix = "kafka-admin")
public class KafkaAdminProperties {

    private boolean bootstrapTopicsOnStartup = true;
    private List<TopicSpec> topics = new ArrayList<>();

    public boolean isBootstrapTopicsOnStartup() { return bootstrapTopicsOnStartup; }
    public void setBootstrapTopicsOnStartup(boolean v) { this.bootstrapTopicsOnStartup = v; }
    public List<TopicSpec> getTopics() { return topics; }
    public void setTopics(List<TopicSpec> topics) { this.topics = topics; }

    public static class TopicSpec {
        private String name;
        private int partitions = 1;
        private short replicationFactor = 1;
        private Map<String, String> configs = new LinkedHashMap<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getPartitions() { return partitions; }
        public void setPartitions(int partitions) { this.partitions = partitions; }
        public short getReplicationFactor() { return replicationFactor; }
        public void setReplicationFactor(short replicationFactor) { this.replicationFactor = replicationFactor; }
        public Map<String, String> getConfigs() { return configs; }
        public void setConfigs(Map<String, String> configs) { this.configs = configs; }
    }
}
