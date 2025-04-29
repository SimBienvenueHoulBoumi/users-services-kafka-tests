package primerriva.users_services.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import primerriva.users_services.dto.UserEventDto;
import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.exceptions.UserAlreadyExistsException;
import primerriva.users_services.exceptions.UserNotFoundException;
import primerriva.users_services.models.Users;
import primerriva.users_services.repositories.UsersRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersServiceImplTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UsersServiceImpl usersService;

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Successfully saves user and publishes CREATED event to Kafka")
        void successfulCreation_savesAndPublishes() throws JsonProcessingException {
            UsersDto usersDto = UsersDto.builder()
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();

            Users.builder()
                    .id(1L)
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();

            when(usersRepository.findByEmail(usersDto.getEmail())).thenReturn(null);
            when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> {
                Users saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(objectMapper.writeValueAsString(any(UserEventDto.class)))
                    .thenReturn("{\"id\":1,\"username\":\"Alice\",\"email\":\"alice@mail.com\",\"action\":\"CREATED\"}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("user-created-topic"), anyString())).thenReturn(futureResult);

            usersService.createUser(usersDto);

            verify(usersRepository).findByEmail(usersDto.getEmail());
            verify(usersRepository).save(any(Users.class));
            verify(kafkaTemplate).send(eq("user-created-topic"), anyString());
        }

        @Test
        @DisplayName("Throws UserAlreadyExistsException when user email already exists")
        void userAlreadyExists_throwsException() {
            UsersDto usersDto = UsersDto.builder()
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();

            Users user = Users.builder()
                    .id(1L)
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();

            when(usersRepository.findByEmail(usersDto.getEmail())).thenReturn(user);

            assertThrows(UserAlreadyExistsException.class, () -> usersService.createUser(usersDto),
                    "Expected UserAlreadyExistsException for duplicate email");
            verify(usersRepository).findByEmail(usersDto.getEmail());
            verify(usersRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when UsersDto is invalid")
        void invalidUsersDto_throwsException() {
            UsersDto invalidDto = UsersDto.builder()
                    .username("")
                    .email("")
                    .build();

            assertThrows(IllegalArgumentException.class, () -> usersService.createUser(invalidDto),
                    "Expected IllegalArgumentException for invalid UsersDto");
            verify(usersRepository, never()).findByEmail(anyString());
            verify(usersRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when UsersDto is null")
        void nullUsersDto_throwsException() {
            assertThrows(IllegalArgumentException.class, () -> usersService.createUser(null),
                    "Expected IllegalArgumentException for null UsersDto");
            verify(usersRepository, never()).findByEmail(anyString());
            verify(usersRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Get User By Email Tests")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Successfully returns user and publishes GET_ONE event")
        void userFound_publishesAndReturns() throws JsonProcessingException {
            Users user = Users.builder()
                    .id(1L)
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();

            when(usersRepository.findByEmail(user.getEmail())).thenReturn(user);
            when(objectMapper.writeValueAsString(any(UserEventDto.class)))
                    .thenReturn("{\"id\":1,\"username\":\"Alice\",\"email\":\"alice@mail.com\",\"action\":\"GET_ONE\"}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("user-get-one-topic"), anyString())).thenReturn(futureResult);

            Users result = usersService.getUserByEmail(user.getEmail());

            assertNotNull(result);
            assertEquals(user.getUsername(), result.getUsername());
            assertEquals(user.getEmail(), result.getEmail());
            verify(usersRepository).findByEmail(user.getEmail());
            verify(kafkaTemplate).send(eq("user-get-one-topic"), anyString());
        }

        @Test
        @DisplayName("Throws UserNotFoundException when user not found")
        void userNotFound_throwsException() {
            String email = "notfound@mail.com";
            when(usersRepository.findByEmail(email)).thenReturn(null);

            assertThrows(UserNotFoundException.class, () -> usersService.getUserByEmail(email),
                    "Expected UserNotFoundException for non-existent email");
            verify(usersRepository).findByEmail(email);
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when email is null or empty")
        void nullOrEmptyEmail_throwsException() {
            assertThrows(IllegalArgumentException.class, () -> usersService.getUserByEmail(null),
                    "Expected IllegalArgumentException for null email");
            assertThrows(IllegalArgumentException.class, () -> usersService.getUserByEmail(""),
                    "Expected IllegalArgumentException for empty email");
            verify(usersRepository, never()).findByEmail(anyString());
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Successfully updates user and publishes UPDATED event")
        void successfulUpdate_updatesAndPublishes() throws JsonProcessingException {
            UsersDto updatedDto = UsersDto.builder()
                    .username("Alice Updated")
                    .email("alice.updated@mail.com")
                    .build();
            Users existingUser = Users.builder()
                    .id(1L)
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();
            Users updatedUser = Users.builder()
                    .id(1L)
                    .username("Alice Updated")
                    .email("alice.updated@mail.com")
                    .password("hashedPassword")
                    .build();

            when(usersRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(usersRepository.save(any(Users.class))).thenReturn(updatedUser);
            when(objectMapper.writeValueAsString(any(UserEventDto.class)))
                    .thenReturn("{\"id\":1,\"username\":\"Alice Updated\",\"email\":\"alice.updated@mail.com\",\"action\":\"UPDATED\"}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("user-updated-topic"), anyString())).thenReturn(futureResult);

            usersService.updateUser(1L, updatedDto);

            verify(usersRepository).findById(1L);
            verify(usersRepository).save(any(Users.class));
            verify(kafkaTemplate).send(eq("user-updated-topic"), anyString());
        }

        @Test
        @DisplayName("Throws UserNotFoundException when user not found")
        void userNotFound_throwsException() {
            UsersDto updatedDto = UsersDto.builder()
                    .username("Alice Updated")
                    .email("alice.updated@mail.com")
                    .build();
            when(usersRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> usersService.updateUser(1L, updatedDto),
                    "Expected UserNotFoundException for non-existent user");
            verify(usersRepository).findById(1L);
            verify(usersRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when UsersDto is null")
        void nullUsersDto_throwsException() {
            assertThrows(IllegalArgumentException.class, () -> usersService.updateUser(1L, null),
                    "Expected IllegalArgumentException for null UsersDto");
            verify(usersRepository, never()).findById(anyLong());
            verify(usersRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Successfully deletes user and publishes DELETED event")
        void successfulDelete_deletesAndPublishes() throws JsonProcessingException {
            Users user = Users.builder()
                    .id(1L)
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();

            when(usersRepository.findById(1L)).thenReturn(Optional.of(user));
            doNothing().when(usersRepository).deleteById(1L);
            when(objectMapper.writeValueAsString(any(UserEventDto.class)))
                    .thenReturn("{\"id\":1,\"email\":\"alice@mail.com\",\"action\":\"DELETED\"}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("user-deleted-topic"), anyString())).thenReturn(futureResult);

            usersService.deleteUser(1L);

            verify(usersRepository).findById(1L);
            verify(usersRepository).deleteById(1L);
            verify(kafkaTemplate).send(eq("user-deleted-topic"), anyString());
        }

        @Test
        @DisplayName("Throws UserNotFoundException when user not found")
        void userNotFound_throwsException() {
            when(usersRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> usersService.deleteUser(1L),
                    "Expected UserNotFoundException for non-existent user");
            verify(usersRepository).findById(1L);
            verify(usersRepository, never()).deleteById(anyLong());
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Kafka Listener Tests")
    class KafkaListenerTests {

        @Test
        @DisplayName("Handles get user by username request and sends response")
        void handleGetUserByUsernameRequest_sendsResponse() throws JsonProcessingException {
            String correlationId = "test-correlation-id";
            String username = "alice@mail.com";
            Users user = Users.builder()
                    .id(1L)
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();
            Map<String, Object> requestPayload = Map.of(
                    "correlationId", correlationId,
                    "username", username
            );
            Map<String, Object> responsePayload = Map.of(
                    "correlationId", correlationId,
                    "success", true,
                    "username", username,
                    "password", "hashedPassword",
                    "roles", List.of("USER")
            );

            when(objectMapper.readValue(eq("request-message"), any(TypeReference.class)))
                    .thenReturn(requestPayload);
            when(usersRepository.findByEmail(username)).thenReturn(user);
            when(objectMapper.writeValueAsString(eq(responsePayload)))
                    .thenReturn("{\"correlationId\":\"test-correlation-id\",\"success\":true,\"username\":\"alice@mail.com\",\"password\":\"hashedPassword\",\"roles\":[\"USER\"]}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("response-user-details"), anyString())).thenReturn(futureResult);

            usersService.handleGetUserByUsernameRequest("request-message");

            verify(usersRepository).findByEmail(username);
            verify(kafkaTemplate).send(eq("response-user-details"), anyString());
        }

        @Test
        @DisplayName("Handles get user by username request with non-existent user")
        void handleGetUserByUsernameRequest_userNotFound() throws JsonProcessingException {
            String correlationId = "test-correlation-id";
            String username = "notfound@mail.com";
            Map<String, Object> requestPayload = Map.of(
                    "correlationId", correlationId,
                    "username", username
            );
            Map<String, Object> errorPayload = Map.of(
                    "correlationId", correlationId,
                    "success", false,
                    "error", "User not found for username: " + username
            );

            when(objectMapper.readValue(eq("request-message"), any(TypeReference.class)))
                    .thenReturn(requestPayload);
            when(usersRepository.findByEmail(username)).thenReturn(null);
            when(objectMapper.writeValueAsString(eq(errorPayload)))
                    .thenReturn("{\"correlationId\":\"test-correlation-id\",\"success\":false,\"error\":\"User not found for username: notfound@mail.com\"}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("response-user-details"), anyString())).thenReturn(futureResult);

            usersService.handleGetUserByUsernameRequest("request-message");

            verify(usersRepository).findByEmail(username);
            verify(kafkaTemplate).send(eq("response-user-details"), anyString());
        }

        @Test
        @DisplayName("Handles create user request and sends response")
        void handleCreateUserRequest_sendsResponse() throws JsonProcessingException {
            String correlationId = "test-correlation-id";
            String email = "alice@mail.com";
            Users user = Users.builder()
                    .id(1L)
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();
            Map<String, Object> requestPayload = Map.of(
                    "correlationId", correlationId,
                    "name", "Alice",
                    "email", email,
                    "password", "hashedPassword"
            );
            Map<String, Object> responsePayload = Map.of(
                    "correlationId", correlationId,
                    "success", true,
                    "username", email,
                    "password", "hashedPassword",
                    "roles", List.of("USER")
            );

            when(objectMapper.readValue(eq("request-message"), any(TypeReference.class)))
                    .thenReturn(requestPayload);
            when(usersRepository.findByEmail(email)).thenReturn(null).thenReturn(user);
            when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> {
                Users saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(objectMapper.writeValueAsString(any(UserEventDto.class)))
                    .thenReturn("{\"id\":1,\"username\":\"Alice\",\"email\":\"alice@mail.com\",\"action\":\"CREATED\"}");
            when(objectMapper.writeValueAsString(eq(responsePayload)))
                    .thenReturn("{\"correlationId\":\"test-correlation-id\",\"success\":true,\"username\":\"alice@mail.com\",\"password\":\"hashedPassword\",\"roles\":[\"USER\"]}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(anyString(), anyString())).thenReturn(futureResult);

            usersService.handleCreateUserRequest("request-message");

            verify(usersRepository, times(2)).findByEmail(email); // Allow two invocations
            verify(usersRepository).save(any(Users.class));
            verify(kafkaTemplate).send(eq("user-created-topic"), anyString());
            verify(kafkaTemplate).send(eq("response-user-details"), anyString());
        }

        @Test
        @DisplayName("Handles create user request with existing user")
        void handleCreateUserRequest_userAlreadyExists() throws JsonProcessingException {
            String correlationId = "test-correlation-id";
            String email = "alice@mail.com";
            Users user = Users.builder()
                    .id(1L)
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();
            Map<String, Object> requestPayload = Map.of(
                    "correlationId", correlationId,
                    "name", "Alice",
                    "email", email,
                    "password", "hashedPassword"
            );
            Map<String, Object> errorPayload = Map.of(
                    "correlationId", correlationId,
                    "success", false,
                    "error", "User already exists: " + email
            );

            when(objectMapper.readValue(eq("request-message"), any(TypeReference.class)))
                    .thenReturn(requestPayload);
            when(usersRepository.findByEmail(email)).thenReturn(user);
            when(objectMapper.writeValueAsString(eq(errorPayload)))
                    .thenReturn("{\"correlationId\":\"test-correlation-id\",\"success\":false,\"error\":\"User already exists: alice@mail.com\"}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("response-user-details"), anyString())).thenReturn(futureResult);

            usersService.handleCreateUserRequest("request-message");

            verify(usersRepository).findByEmail(email);
            verify(usersRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(eq("user-created-topic"), anyString());
            verify(kafkaTemplate).send(eq("response-user-details"), anyString());
        }

        @Test
        @DisplayName("Handles create user request with invalid user data")
        void handleCreateUserRequest_invalidUserData() throws JsonProcessingException {
            String correlationId = "test-correlation-id";
            String email = "";
            Map<String, Object> requestPayload = Map.of(
                    "correlationId", correlationId,
                    "name", "",
                    "email", email,
                    "password", "hashedPassword"
            );
            Map<String, Object> errorPayload = Map.of(
                    "correlationId", correlationId,
                    "success", false,
                    "error", "Name must not be null or empty"
            );

            when(objectMapper.readValue(eq("request-message"), any(TypeReference.class)))
                    .thenReturn(requestPayload);
            when(objectMapper.writeValueAsString(eq(errorPayload)))
                    .thenReturn("{\"correlationId\":\"test-correlation-id\",\"success\":false,\"error\":\"Name must not be null or empty\"}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("response-user-details"), anyString())).thenReturn(futureResult);

            usersService.handleCreateUserRequest("request-message");

            verify(usersRepository, never()).findByEmail(anyString());
            verify(usersRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(eq("user-created-topic"), anyString());
            verify(kafkaTemplate).send(eq("response-user-details"), anyString());
        }

        @Test
        @DisplayName("Handles create user request with missing correlationId")
        void handleCreateUserRequest_missingCorrelationId() throws JsonProcessingException {
            Map<String, Object> requestPayload = Map.of(
                    "name", "Alice",
                    "email", "alice@mail.com",
                    "password", "hashedPassword"
            );
            Map<String, Object> errorPayload = Map.of(
                    "correlationId", "unknown",
                    "success", false,
                    "error", "Missing or invalid correlationId"
            );

            when(objectMapper.readValue(eq("request-message"), any(TypeReference.class)))
                    .thenReturn(requestPayload);
            when(objectMapper.writeValueAsString(eq(errorPayload)))
                    .thenReturn("{\"correlationId\":\"unknown\",\"success\":false,\"error\":\"Missing or invalid correlationId\"}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("response-user-details"), anyString())).thenReturn(futureResult);

            usersService.handleCreateUserRequest("request-message");

            verify(usersRepository, never()).findByEmail(anyString());
            verify(usersRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(eq("user-created-topic"), anyString());
            verify(kafkaTemplate).send(eq("response-user-details"), anyString());
        }
    }
}