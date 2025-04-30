package primerriva.users_services.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import primerriva.users_services.kafka.KafkaTopics;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topic.partitions:1}")
    private int partitions;

    @Value("${kafka.topic.replication-factor:1}")
    private short replicationFactor;

    public static final String GET_USER_BY_USERNAME = "request-get-user-by-username";
    public static final String CREATE_USER = "request-user-create-topic";
    public static final String GET_ONE_USER = "request-user-get-one-topic";
    public static final String UPDATE_USER = "request-user-updated-topic";
    public static final String DELETE_USER = "request-user-deleted-topic";

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public NewTopic userByUsernameTopic() {
        return new NewTopic(KafkaTopics.GET_USER_BY_USERNAME, partitions, replicationFactor);
    }

    @Bean
    public NewTopic createUserTopic() {
        return new NewTopic(KafkaTopics.CREATE_USER, partitions, replicationFactor);
    }

    @Bean
    public NewTopic getOneUserTopic() {
        return new NewTopic(KafkaTopics.GET_ONE_USER, partitions, replicationFactor);
    }

    @Bean
    public NewTopic updateUserTopic() {
        return new NewTopic(KafkaTopics.UPDATE_USER, partitions, replicationFactor);
    }

    @Bean
    public NewTopic deleteUserTopic() {
        return new NewTopic(KafkaTopics.DELETE_USER, partitions, replicationFactor);
    }
}