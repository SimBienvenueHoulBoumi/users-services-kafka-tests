package primerriva.users_services.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import primerriva.users_services.dto.UserEventDto;
import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.exceptions.UserAlreadyExistsException;
import primerriva.users_services.exceptions.UserNotFoundException;
import primerriva.users_services.mapper.UsersMapper;
import primerriva.users_services.models.Users;
import primerriva.users_services.repositories.UsersRepository;
import primerriva.users_services.services.UsersService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class UsersServiceImpl implements UsersService {

    private static final Logger logger = LoggerFactory.getLogger(UsersServiceImpl.class);

    private final UsersRepository usersRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String USER_GET_ONE_TOPIC = "user-get-one-topic";
    private static final String USER_CREATED_TOPIC = "user-created-topic";
    private static final String USER_UPDATED_TOPIC = "user-updated-topic";
    private static final String USER_DELETED_TOPIC = "user-deleted-topic";
    private static final String RESPONSE_USER_DETAILS_TOPIC = "response-user-details";

    @Override
    public Users getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }

        Users user = usersRepository.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException(email);
        }

        UserEventDto eventDto = UserEventDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .action("GET_ONE")
                .timestamp(LocalDateTime.now())
                .build();
        sendToKafka(USER_GET_ONE_TOPIC, eventDto);

        return user;
    }

    @Override
    public void createUser(UsersDto userDto) {
        if (userDto == null) {
            throw new IllegalArgumentException("User DTO must not be null");
        }

        Users newUser;
        try {
            newUser = UsersMapper.toEntity(userDto); // Validates username and email
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user data: " + e.getMessage(), e);
        }

        if (usersRepository.findByEmail(userDto.getEmail()) != null) {
            throw new UserAlreadyExistsException(userDto.getEmail());
        }

        newUser = usersRepository.save(newUser);

        UserEventDto eventDto = UserEventDto.builder()
                .id(newUser.getId())
                .username(newUser.getUsername())
                .email(newUser.getEmail())
                .action("CREATED")
                .timestamp(LocalDateTime.now())
                .build();
        sendToKafka(USER_CREATED_TOPIC, eventDto);
    }

    @Override
    public void updateUser(Long id, UsersDto userDto) {
        if (userDto == null) {
            throw new IllegalArgumentException("User DTO must not be null");
        }

        Users existingUser = usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        Users updatedUser;
        try {
            updatedUser = UsersMapper.toEntity(userDto);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user data: " + e.getMessage(), e);
        }

        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        // Password updates should be handled separately if needed
        usersRepository.save(existingUser);

        UserEventDto eventDto = UserEventDto.builder()
                .id(id)
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .action("UPDATED")
                .timestamp(LocalDateTime.now())
                .build();
        sendToKafka(USER_UPDATED_TOPIC, eventDto);
    }

    @Override
    public void deleteUser(Long id) {
        Users userToDelete = usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        usersRepository.deleteById(id);

        UserEventDto eventDto = UserEventDto.builder()
                .id(id)
                .email(userToDelete.getEmail())
                .action("DELETED")
                .timestamp(LocalDateTime.now())
                .build();
        sendToKafka(USER_DELETED_TOPIC, eventDto);
    }

    @KafkaListener(topics = "request-get-user-by-username", groupId = "users-service-group")
    public void handleGetUserByUsernameRequest(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {});
            String correlationId = (String) payload.get("correlationId");
            String username = (String) payload.get("username");

            if (correlationId == null || correlationId.isBlank()) {
                logger.warn("Missing or invalid correlationId in get user request");
                return;
            }
            if (username == null || username.isBlank()) {
                sendErrorResponse(correlationId, "Username must not be null or empty");
                return;
            }

            Users user = usersRepository.findByEmail(username);
            if (user == null) {
                sendErrorResponse(correlationId, "User not found for username: " + username);
                return;
            }

            Map<String, Object> response = Map.of(
                    "correlationId", correlationId,
                    "success", true,
                    "username", user.getEmail(),
                    "password", user.getPassword(),
                    "roles", List.of("USER")
            );
            sendToKafka(RESPONSE_USER_DETAILS_TOPIC, response);

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse get user request message: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error handling get user by username request: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "request-create-user", groupId = "users-service-group")
    public void handleCreateUserRequest(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {});
            String correlationId = (String) payload.get("correlationId");
            String name = (String) payload.get("name");
            String email = (String) payload.get("email");
            String password = (String) payload.get("password");

            // Validate input
            if (correlationId == null || correlationId.isBlank()) {
                sendErrorResponse("unknown", "Missing or invalid correlationId");
                return;
            }
            if (name == null || name.isBlank()) {
                sendErrorResponse(correlationId, "Name must not be null or empty");
                return;
            }
            if (email == null || email.isBlank()) {
                sendErrorResponse(correlationId, "Email must not be null or empty");
                return;
            }

            UsersDto userDto = UsersDto.builder()
                    .username(name)
                    .email(email)
                    .password(password)
                    .build();

            try {
                createUser(userDto);
                Users user = usersRepository.findByEmail(email);
                Map<String, Object> response = Map.of(
                        "correlationId", correlationId,
                        "success", true,
                        "username", user.getEmail(),
                        "password", user.getPassword(),
                        "roles", List.of("USER")
                );
                sendToKafka(RESPONSE_USER_DETAILS_TOPIC, response);
            } catch (UserAlreadyExistsException e) {
                sendErrorResponse(correlationId, "User already exists: " + email);
            } catch (IllegalArgumentException e) {
                sendErrorResponse(correlationId, "Invalid user data: " + e.getMessage());
            }

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse create user request message: {}", e.getMessage(), e);
            try {
                Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {});
                String correlationId = (String) payload.get("correlationId");
                sendErrorResponse(correlationId != null ? correlationId : "unknown", "Failed to parse request: " + e.getMessage());
            } catch (JsonProcessingException ex) {
                logger.error("Failed to extract correlationId for error response: {}", ex.getMessage(), ex);
            }
        } catch (Exception e) {
            logger.error("Error handling create user request: {}", e.getMessage(), e);
            try {
                Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {});
                String correlationId = (String) payload.get("correlationId");
                sendErrorResponse(correlationId != null ? correlationId : "unknown", "Failed to create user: " + e.getMessage());
            } catch (JsonProcessingException ex) {
                logger.error("Failed to extract correlationId for error response: {}", ex.getMessage(), ex);
            }
        }
    }

    private void sendErrorResponse(String correlationId, String errorMessage) {
        Map<String, Object> response = Map.of(
                "correlationId", correlationId != null ? correlationId : "unknown",
                "success", false,
                "error", errorMessage
        );
        try {
            sendToKafka(RESPONSE_USER_DETAILS_TOPIC, response);
        } catch (Exception e) {
            logger.error("Failed to send error response for correlationId {}: {}", correlationId, e.getMessage(), e);
        }
    }

    private void sendToKafka(String topic, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            logger.debug("Sending message to topic {}: {}", topic, json);
            kafkaTemplate.send(topic, json);
        } catch (JsonProcessingException e) {
            logger.error("Failed to send message to topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Error sending Kafka message", e);
        }
    }
}