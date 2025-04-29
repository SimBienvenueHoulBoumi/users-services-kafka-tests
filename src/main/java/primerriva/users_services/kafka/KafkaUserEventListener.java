package primerriva.users_services.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.models.Users;
import primerriva.users_services.services.UsersService;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaUserEventListener {

    private final UsersService usersService;
    private final KafkaMessageProcessor kafkaMessageProcessor;

    private static final String REQUEST_GET_ONE_USER_TOPIC = "request-user-get-one-topic";
    private static final String REQUEST_CREATE_USER_TOPIC = "request-user-created-topic";
    private static final String REQUEST_UPDATE_USER_TOPIC = "request-user-updated-topic";
    private static final String REQUEST_DELETE_USER_TOPIC = "request-user-deleted-topic";
    private static final String REQUEST_GET_USER_BY_USERNAME_TOPIC = "request-user-get-by-username-topic";

    @KafkaListener(topics = REQUEST_GET_USER_BY_USERNAME_TOPIC, groupId = "users-service-group")
    public void handleGetUserByUsernameRequest(String message) {
        kafkaMessageProcessor.process(message, "get user by username", payload -> {
            String correlationId = kafkaMessageProcessor.getRequiredString(payload, "correlationId");
            String username = kafkaMessageProcessor.getRequiredString(payload, "username");

            Users user = usersService.getUserByEmail(username);
            kafkaMessageProcessor.sendSuccessResponse(correlationId, Map.of(
                    "username", user.getEmail(),
                    "password", user.getPassword(),
                    "roles", List.of("USER")
            ));
        });
    }

    @KafkaListener(topics = REQUEST_CREATE_USER_TOPIC, groupId = "users-service-group")
    public void handleCreateUserRequest(String message) {
        kafkaMessageProcessor.process(message, "create user", payload -> {
            String correlationId = kafkaMessageProcessor.getRequiredString(payload, "correlationId");
            String name = kafkaMessageProcessor.getRequiredString(payload, "name");
            String email = kafkaMessageProcessor.getRequiredString(payload, "email");
            String password = (String) payload.get("password");

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
        });
    }

    @KafkaListener(topics = REQUEST_GET_ONE_USER_TOPIC, groupId = "users-service-group")
    public void handleGetOneUserRequest(String message) {
        kafkaMessageProcessor.process(message, "get one user", payload -> {
            String correlationId = kafkaMessageProcessor.getRequiredString(payload, "correlationId");
            String email = kafkaMessageProcessor.getRequiredString(payload, "email");

            Users user = usersService.getUserByEmail(email);
            kafkaMessageProcessor.sendSuccessResponse(correlationId, Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "roles", List.of("USER")
            ));
        });
    }

    @KafkaListener(topics = REQUEST_UPDATE_USER_TOPIC, groupId = "users-service-group")
    public void handleUpdateUserRequest(String message) {
        kafkaMessageProcessor.process(message, "update user", payload -> {
            String correlationId = kafkaMessageProcessor.getRequiredString(payload, "correlationId");
            Long id = kafkaMessageProcessor.getRequiredLong(payload, "id");
            String username = kafkaMessageProcessor.getRequiredString(payload, "username");
            String email = kafkaMessageProcessor.getRequiredString(payload, "email");
            String password = (String) payload.get("password");

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
        });
    }

    @KafkaListener(topics = REQUEST_DELETE_USER_TOPIC, groupId = "users-service-group")
    public void handleDeleteUserRequest(String message) {
        kafkaMessageProcessor.process(message, "delete user", payload -> {
            String correlationId = kafkaMessageProcessor.getRequiredString(payload, "correlationId");
            Long id = kafkaMessageProcessor.getRequiredLong(payload, "id");

            usersService.deleteUser(id);
            kafkaMessageProcessor.sendSuccessResponse(correlationId, Map.of(
                    "id", id,
                    "message", "User deleted successfully"
            ));
        });
    }
}