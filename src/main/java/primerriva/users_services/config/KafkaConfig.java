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

import com.fasterxml.jackson.databind.JsonSerializable;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private static final String REQUEST_GET_ONE_USER_TOPIC = "request-user-get-one-topic";
    private static final String REQUEST_CREATE_USER_TOPIC = "request-user-created-topic";
    private static final String REQUEST_UPDATE_USER_TOPIC = "request-user-updated-topic";
    private static final String REQUEST_DELETE_USER_TOPIC = "request-user-deleted-topic";
    private static final String REQUEST_GET_USER_BY_USERNAME_TOPIC = "request-user-get-by-username-topic";
    private static final String RESPONSE_USER_DETAILS_TOPIC = "response-user-details-topic";

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializable.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public NewTopic getUserByUsernameTopic() {
        return new NewTopic(REQUEST_GET_USER_BY_USERNAME_TOPIC, 1, (short) 1);
    }

    @Bean
    public NewTopic createUserTopic() {
        return new NewTopic(REQUEST_CREATE_USER_TOPIC, 1, (short) 1);
    }

    @Bean
    public NewTopic getOneUserTopic() {
        return new NewTopic(REQUEST_GET_ONE_USER_TOPIC, 1, (short) 1);
    }

    @Bean
    public NewTopic updateUserTopic() {
        return new NewTopic(REQUEST_UPDATE_USER_TOPIC, 1, (short) 1);
    }

    @Bean
    public NewTopic deleteUserTopic() {
        return new NewTopic(REQUEST_DELETE_USER_TOPIC, 1, (short) 1);
    }

    @Bean
    public NewTopic responseUserDetailsTopic() {
        return new NewTopic(RESPONSE_USER_DETAILS_TOPIC, 1, (short) 1);
    }
}