package primerriva.users_services.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import primerriva.users_services.exceptions.UserAlreadyExistsException;
import primerriva.users_services.exceptions.UserNotFoundException;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaMessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageProcessor.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String RESPONSE_USER_DETAILS_TOPIC = "response-user-details-topic";

    @FunctionalInterface
    public interface MessageProcessor {
        void process(Map<String, Object> payload) throws Exception;
    }

    public void process(String message, String operation, MessageProcessor processor) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {});
            processor.process(payload);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse {} request message: {}", operation, e.getMessage());
            sendErrorResponse(extractCorrelationId(message), "Failed to parse request: " + e.getMessage());
        } catch (IllegalArgumentException | UserNotFoundException | UserAlreadyExistsException e) {
            sendErrorResponse(extractCorrelationId(message), e.getMessage());
        } catch (Exception e) {
            logger.error("Error handling {} request: {}", operation, e.getMessage(), e);
            sendErrorResponse(extractCorrelationId(message), "Failed to process " + operation + ": " + e.getMessage());
        }
    }

    public String extractCorrelationId(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {});
            return (String) payload.get("correlationId");
        } catch (JsonProcessingException e) {
            logger.error("Failed to extract correlationId: {}", e.getMessage());
            return "unknown";
        }
    }

    public String getRequiredString(Map<String, Object> payload, String field) {
        String value = (String) payload.get(field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be null or empty");
        }
        return value;
    }

    public Long getRequiredLong(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a valid number");
        }
    }

    public void sendSuccessResponse(String correlationId, Map<String, Object> data) {
        Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "success", true,
                "data", data
        );
        sendToKafka(RESPONSE_USER_DETAILS_TOPIC, response);
    }

    public void sendErrorResponse(String correlationId, String errorMessage) {
        Map<String, Object> response = Map.of(
                "correlationId", correlationId != null ? correlationId : "unknown",
                "success", false,
                "error", errorMessage
        );
        sendToKafka(RESPONSE_USER_DETAILS_TOPIC, response);
    }

    private void sendToKafka(String topic, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            logger.debug("Sending message to topic {}: {}", topic, json);
            kafkaTemplate.send(topic, json);
        } catch (JsonProcessingException e) {
            logger.error("Failed to send message to topic {}: {}", topic, e.getMessage());
            throw new RuntimeException("Error sending Kafka message", e);
        }
    }
}