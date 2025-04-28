package primerriva.users_services.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the UsersServiceImpl class. 
 * This class contains tests for user-related operations such as
 * creating, retrieving, and deleting users. 
 * Each test checks the interaction with the user repository, 
 * the publishing 
 * of events to Kafka, and the handling of exceptions when operations fail.
 */
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

    private UsersDto usersDto;
    private Users user;

    @BeforeEach
    void setUp() {
        usersDto = UsersDto.builder()
                .name("Alice")
                .email("alice@mail.com")
                .build();

        user = Users.builder()
                .id(1L)
                .name("Alice")
                .email("alice@mail.com")
                .build();
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Successfully saves and publishes to Kafka")
        void successfulCreation_savesAndPublishes() throws JsonProcessingException {
            when(usersRepository.findByEmail(usersDto.getEmail())).thenReturn(null);
            when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> {
                Users saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(objectMapper.writeValueAsString(any(UserEventDto.class)))
                    .thenReturn("{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@mail.com\",\"action\":\"CREATED\"}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("user-created-topic"), anyString()))
                    .thenReturn(futureResult);

            usersService.createUser(usersDto);

            verify(usersRepository).save(any(Users.class));
            verify(kafkaTemplate).send(eq("user-created-topic"), anyString());
        }

        @Test
        @DisplayName("Throws exception when user already exists")
        void userAlreadyExists_throwsException() {
            when(usersRepository.findByEmail(usersDto.getEmail())).thenReturn(user);

            assertThrows(UserAlreadyExistsException.class, () -> usersService.createUser(usersDto));
            verify(usersRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Get User By Email Tests")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Publishes and returns user when found")
        void userFound_publishesAndReturns() throws JsonProcessingException {
            when(usersRepository.findByEmail(user.getEmail())).thenReturn(user);
            when(objectMapper.writeValueAsString(any(UserEventDto.class)))
                    .thenReturn("{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@mail.com\",\"action\":\"GET_ONE\"}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("user-get-one-topic"), anyString()))
                    .thenReturn(futureResult);

            Users result = usersService.getUserByEmail(user.getEmail());

            assertNotNull(result);
            assertEquals(user.getName(), result.getName());
            assertEquals(user.getEmail(), result.getEmail());
            verify(kafkaTemplate).send(eq("user-get-one-topic"), anyString());
        }

        @Test
        @DisplayName("Throws exception when user not found")
        void userNotFound_throwsException() {
            String email = "notfound@mail.com";
            when(usersRepository.findByEmail(email)).thenReturn(null);

            assertThrows(UserNotFoundException.class, () -> usersService.getUserByEmail(email));
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Successfully deletes and publishes to Kafka")
        void successfulDelete_deletesAndPublishes() throws JsonProcessingException {
            when(usersRepository.findById(user.getId())).thenReturn(Optional.of(user));
            doNothing().when(usersRepository).deleteById(user.getId());

            when(objectMapper.writeValueAsString(any(UserEventDto.class)))
                    .thenReturn("{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@mail.com\",\"action\":\"DELETED\"}");

            CompletableFuture<SendResult<String, String>> futureResult = CompletableFuture.completedFuture(mock(SendResult.class));
            when(kafkaTemplate.send(eq("user-deleted-topic"), anyString())).thenReturn(futureResult);

            usersService.deleteUser(user.getId());

            verify(usersRepository).findById(user.getId());
            verify(usersRepository).deleteById(user.getId());
            verify(kafkaTemplate).send(eq("user-deleted-topic"), anyString());
        }

        @Test
        @DisplayName("Throws exception when user not found")
        void userNotFound_throwsException() {
            when(usersRepository.findById(user.getId())).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> usersService.deleteUser(user.getId()));
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }
    }
}
