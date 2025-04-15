package primerriva.users_services.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.exceptions.UserAlreadyExistsException;
import primerriva.users_services.exceptions.UserNotFoundException;
import primerriva.users_services.models.Users;
import primerriva.users_services.repositories.UsersRepository;

class UsersServiceImplTest {

    private UsersRepository usersRepository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private ObjectMapper objectMapper;
    private UsersServiceImpl usersService;

    @BeforeEach
    void setUp() {
        usersRepository = mock(UsersRepository.class);
        objectMapper = new ObjectMapper();
        usersService = new UsersServiceImpl(usersRepository, kafkaTemplate, objectMapper);
    }

    @Test
    void shouldCreateUserSuccessfully() {
        UsersDto dto = new UsersDto("Alice", "alice@mail.com");

        when(usersRepository.findByEmail(dto.getEmail())).thenReturn(null);
        when(usersRepository.save(any(Users.class))).thenAnswer(i -> i.getArgument(0));

        usersService.createUser(dto);

        verify(usersRepository).save(any(Users.class));
        verify(kafkaTemplate).send(eq("user-created-topic"), any(String.class));
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyExists() {
        UsersDto dto = new UsersDto("Bob", "bob@mail.com");
        Users existingUser = new Users();
        existingUser.setEmail(dto.getEmail());

        when(usersRepository.findByEmail(dto.getEmail())).thenReturn(existingUser);

        assertThrows(UserAlreadyExistsException.class, () -> usersService.createUser(dto));
    }

    @Test
    void shouldPublishUserWhenFoundByEmail() {
        String email = "test@mail.com";
        Users user = new Users();
        user.setName("Test");
        user.setEmail(email);

        when(usersRepository.findByEmail(email)).thenReturn(user);

        usersService.getUserByEmail(email);

        verify(kafkaTemplate).send(eq("user-get-one-topic"), any(String.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundByEmail() {
        String email = "notfound@mail.com";
        when(usersRepository.findByEmail(email)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> usersService.getUserByEmail(email));
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        Long id = 1L;
        Users user = new Users();
        user.setId(id);
        user.setEmail("old@mail.com");
        user.setName("Old");

        when(usersRepository.findById(id)).thenReturn(Optional.of(user));

        UsersDto updatedDto = new UsersDto("NewName", "new@mail.com");
        usersService.updateUser(id, updatedDto);

        verify(usersRepository).save(any(Users.class));
        verify(kafkaTemplate).send(eq("user-updated-topic"), any(String.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundById() {
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> usersService.updateUser(1L, new UsersDto("Name", "email@mail.com")));
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        Long id = 1L;
        Users user = new Users();
        user.setId(id);
        user.setEmail("deleted@mail.com");

        when(usersRepository.findById(id)).thenReturn(Optional.of(user));

        usersService.deleteUser(id);

        verify(usersRepository).deleteById(id);
        verify(kafkaTemplate).send(eq("user-deleted-topic"), contains("\"action\":\"DELETED\""));
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonexistentUser() {
        Long id = 999L;
        when(usersRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> usersService.deleteUser(id));
    }
}
