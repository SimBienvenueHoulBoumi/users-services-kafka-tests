package primerriva.users_services.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.exceptions.UserAlreadyExistsException;
import primerriva.users_services.exceptions.UserNotFoundException;
import primerriva.users_services.models.Users;
import primerriva.users_services.repositories.UsersRepository;

class UsersServiceImplTest {

    private UsersRepository usersRepository;
    private UsersServiceImpl usersService;

    @BeforeEach
    void setUp() {
        usersRepository = mock(UsersRepository.class);
        usersService = new UsersServiceImpl(usersRepository);
    }

    @Test
    void shouldCreateUserSuccessfully() {
        UsersDto dto = new UsersDto("Alice", "alice@mail.com");
        Users user = new Users();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        when(usersRepository.findByEmail(dto.getEmail())).thenReturn(null);
        when(usersRepository.save(any(Users.class))).thenReturn(user);

        Users result = usersService.createUser(dto);

        assertEquals(dto.getName(), result.getName());
        assertEquals(dto.getEmail(), result.getEmail());
        verify(usersRepository).save(any(Users.class));
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
    void shouldReturnUserByEmailSuccessfully() {
        String email = "test@mail.com";
        Users user = new Users();
        user.setName("Test");
        user.setEmail(email);

        when(usersRepository.findByEmail(email)).thenReturn(user);

        Users result = usersService.getUserByEmail(email);
        assertEquals(user.getName(), result.getName());
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundByEmail() {
        String email = "notfound@mail.com";
        when(usersRepository.findByEmail(email)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> usersService.getUserByEmail(email));
    }

    @Test
    void shouldReturnUserByEmail() {
        Long id = 1L;
        Users user = new Users();
        user.setId(id);
        user.setName("Test");
        user.setEmail("test@mail.com");

        when(usersRepository.findByEmail("test@mail.com")).thenReturn(user);

        Users result = usersService.getUserByEmail("test@mail.com");

        assertNotNull(result);
        assertEquals("Test", result.getName());
        assertEquals("test@mail.com", result.getEmail());
    }


    @Test
    void shouldThrowExceptionWhenUserNotFoundById() {
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> usersService.updateUser(1L, new UsersDto("Name", "email@mail.com")));
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        Long id = 1L;
        when(usersRepository.existsById(id)).thenReturn(true);
        doNothing().when(usersRepository).deleteById(id);

        usersService.deleteUser(id);

        verify(usersRepository).deleteById(id);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonexistentUser() {
        Long id = 999L;
        when(usersRepository.existsById(id)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> usersService.deleteUser(id));
    }
}
