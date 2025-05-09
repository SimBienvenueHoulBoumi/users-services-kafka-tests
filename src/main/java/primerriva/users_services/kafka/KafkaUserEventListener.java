package primerriva.users_services.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.models.Users;
import primerriva.users_services.services.UsersService;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUserEventListener {

    private final UsersService usersService;
    private final KafkaMessageProcessor kafkaMessageProcessor;

    @KafkaListener(topics = KafkaTopics.GET_USER_BY_USERNAME, groupId = "users-service-group")
    public void handleGetUserByUsernameRequest(String message) {
        kafkaMessageProcessor.process(message, "get user by username", payload -> {
            String correlationId = kafkaMessageProcessor.getRequiredString(payload, "correlationId");
            String username = kafkaMessageProcessor.getRequiredString(payload, "username");
            log.debug("Processing get user by username: {}", username);

            Users user = usersService.getUserByEmail(username);
            kafkaMessageProcessor.sendSuccessResponse(correlationId, Map.of(
                "username", user.getEmail(),
                "password", user.getPassword(),
                "roles", List.of("USER")
            ));
            return null;
        });
    }

    @KafkaListener(topics = KafkaTopics.CREATE_USER, groupId = "users-service-group")
    public void handleCreateUserRequest(String message) {
        kafkaMessageProcessor.process(message, "create user", payload -> {
            String correlationId = kafkaMessageProcessor.getRequiredString(payload, "correlationId");
            String name = kafkaMessageProcessor.getRequiredString(payload, "name");
            String email = kafkaMessageProcessor.getRequiredString(payload, "email");
            String password = kafkaMessageProcessor.getRequiredString(payload, "password");
            log.debug("Creating user: name={}, email={}", name, email);

            UsersDto userDto = UsersDto.builder()
                .username(name)
                .email(email)
                .password(password)
                .build();
            usersService.createUser(userDto);
            Users user = usersService.getUserByEmail(email);
            kafkaMessageProcessor.sendSuccessResponse(correlationId, Map.of(
                "username", user.getEmail(),
                "password", user.getPassword(),
                "roles", List.of("USER")
            ));
            return null;
        });
    }

    @KafkaListener(topics = KafkaTopics.GET_ONE_USER, groupId = "users-service-group")
    public void handleGetOneUserRequest(String message) {
        kafkaMessageProcessor.process(message, "get one user", payload -> {
            String correlationId = kafkaMessageProcessor.getRequiredString(payload, "correlationId");
            String email = kafkaMessageProcessor.getRequiredString(payload, "email");
            log.debug("Processing get user by email: {}", email);

            Users user = usersService.getUserByEmail(email);
            kafkaMessageProcessor.sendSuccessResponse(correlationId, Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", List.of("USER")
            ));
            return null;
        });
    }

    @KafkaListener(topics = KafkaTopics.UPDATE_USER, groupId = "users-service-group")
    public void handleUpdateUserRequest(String message) {
        kafkaMessageProcessor.process(message, "update user", payload -> {
            String correlationId = kafkaMessageProcessor.getRequiredString(payload, "correlationId");
            Long id = kafkaMessageProcessor.getRequiredLong(payload, "id");
            String username = kafkaMessageProcessor.getRequiredString(payload, "username");
            String email = kafkaMessageProcessor.getRequiredString(payload, "email");
            String password = kafkaMessageProcessor.getRequiredString(payload, "password");
            log.debug("Updating user: id={}, username={}, email={}", id, username, email);

            UsersDto userDto = UsersDto.builder()
                .username(username)
                .email(email)
                .password(password)
                .build();

            usersService.updateUser(id, userDto);
            kafkaMessageProcessor.sendSuccessResponse(correlationId, Map.of(
                "id", id,
                "username", username,
                "email", email,
                "roles", List.of("USER")
            ));
            return null;
        });
    }

    @KafkaListener(topics = KafkaTopics.DELETE_USER, groupId = "users-service-group")
    public void handleDeleteUserRequest(String message) {
        kafkaMessageProcessor.process(message, "delete user", payload -> {
            String correlationId = kafkaMessageProcessor.getRequiredString(payload, "correlationId");
            Long id = kafkaMessageProcessor.getRequiredLong(payload, "id");
            log.debug("Deleting user: id={}", id);

            usersService.deleteUser(id);
            kafkaMessageProcessor.sendSuccessResponse(correlationId, Map.of(
                "id", id,
                "message", "User deleted successfully"
            ));
            return null;
        });
    }
}
