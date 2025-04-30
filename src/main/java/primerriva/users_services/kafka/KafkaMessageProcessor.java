package primerriva.users_services.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class KafkaMessageProcessor {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public void process(String message, String operation, Function<Map<String, Object>, Object> processor) {
        Map<String, Object> payload = objectMapper.readValue(message, Map.class);
        processor.apply(payload);
    }

    public String getRequiredString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || !(value instanceof String)) {
            throw new IllegalArgumentException("Missing or invalid '" + key + "' in payload");
        }
        return (String) value;
    }

    public Long getRequiredLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || !(value instanceof Number)) {
            throw new IllegalArgumentException("Missing or invalid '" + key + "' in payload");
        }
        return ((Number) value).longValue();
    }

    @SneakyThrows
    public void sendSuccessResponse(String correlationId, Map<String, Object> response) {
        String payload = objectMapper.writeValueAsString(response);
        kafkaTemplate.send("response-topic", correlationId, payload);
    }
}