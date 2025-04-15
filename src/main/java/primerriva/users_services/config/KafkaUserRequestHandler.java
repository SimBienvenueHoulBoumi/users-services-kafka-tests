package primerriva.users_services.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.services.UsersService;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUserRequestHandler {

    private final UsersService usersService;
    private final ObjectMapper objectMapper;

    public void handleUserRequest(String message, String action) {
        try {
            switch (action) {
                case "createUser" -> {
                    UsersDto dto = objectMapper.readValue(message, UsersDto.class);
                    log.info("📩 Requête Kafka reçue: createUser({})", dto);
                    usersService.createUser(dto);
                }
                case "updateUser" -> {
                    JsonNode node = objectMapper.readTree(message);
                    Long id = node.get("id").asLong();
                    UsersDto dto = objectMapper.treeToValue(node.get("user"), UsersDto.class);
                    log.info("📩 Requête Kafka reçue: updateUser({}, {})", id, dto);
                    usersService.updateUser(id, dto);
                }
                case "getUserByEmail" -> {
                    JsonNode node = objectMapper.readTree(message);
                    String email = node.get("email").asText();
                    log.info("📩 Requête Kafka reçue: getUserByEmail({})", email);
                    usersService.getUserByEmail(email);
                }
                case "deleteUser" -> {
                    JsonNode node = objectMapper.readTree(message);
                    Long id = node.get("id").asLong();
                    log.info("📩 Requête Kafka reçue: deleteUser({})", id);
                    usersService.deleteUser(id);
                }
                default -> log.warn("🚫 Action non reconnue: {}", action);
            }
        } catch (Exception e) {
            log.error("❌ Erreur dans handleUserRequest pour l'action: {}", action, e);
        }
    }
}
