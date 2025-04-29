package primerriva.users_services.serviceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.exceptions.UserAlreadyExistsException;
import primerriva.users_services.exceptions.UserNotFoundException;
import primerriva.users_services.mapper.UsersMapper;
import primerriva.users_services.models.Users;
import primerriva.users_services.repositories.UsersRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersServiceImplTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private UsersMapper usersMapper;


    @InjectMocks
    private UsersServiceImpl usersService;

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @DisplayName("Successfully creates user")
        void successfulCreation_savesUser() {
        UsersDto usersDto = UsersDto.builder()
                .username("Alice")
                .email("alice@mail.com")
                .password("hashedPassword")
                .build();

        Users mappedUser = Users.builder()
                .username("Alice")
                .email("alice@mail.com")
                .password("hashedPassword")
                .build();

        when(usersRepository.findByEmail(usersDto.getEmail())).thenReturn(null);

        try (MockedStatic<UsersMapper> mockedStatic = mockStatic(UsersMapper.class)) {
                mockedStatic.when(() -> UsersMapper.toEntity(usersDto)).thenReturn(mappedUser);

                when(usersRepository.save(mappedUser)).thenAnswer(invocation -> {
                Users saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
                });

                usersService.createUser(usersDto);

                verify(usersRepository).findByEmail(usersDto.getEmail());
                mockedStatic.verify(() -> UsersMapper.toEntity(usersDto));
                verify(usersRepository).save(mappedUser);
        }
        }

        @Test
        @DisplayName("Throws UserAlreadyExistsException when email already exists")
        void userAlreadyExists_throwsException() {
            UsersDto usersDto = UsersDto.builder()
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();
            Users existingUser = Users.builder()
                    .id(1L)
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();

            when(usersRepository.findByEmail(usersDto.getEmail())).thenReturn(existingUser);

            assertThrows(UserAlreadyExistsException.class, () -> usersService.createUser(usersDto),
                    "Expected UserAlreadyExistsException for duplicate email");
            verify(usersRepository).findByEmail(usersDto.getEmail());
            verify(usersRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when UsersDto is null")
        void nullUsersDto_throwsException() {
            assertThrows(IllegalArgumentException.class, () -> usersService.createUser(null),
                    "Expected IllegalArgumentException for null UsersDto");
            verify(usersRepository, never()).findByEmail(anyString());
            verify(usersRepository, never()).save(any());
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
            verify(usersRepository).findByEmail(invalidDto.getEmail());
            verify(usersRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get User By Email Tests")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Successfully returns user")
        void userFound_returnsUser() {
            Users user = Users.builder()
                    .id(1L)
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();

            when(usersRepository.findByEmail(user.getEmail())).thenReturn(user);

            Users result = usersService.getUserByEmail(user.getEmail());

            assertNotNull(result);
            assertEquals(user.getUsername(), result.getUsername());
            assertEquals(user.getEmail(), result.getEmail());
            verify(usersRepository).findByEmail(user.getEmail());
        }

        @Test
        @DisplayName("Throws UserNotFoundException when user not found")
        void userNotFound_throwsException() {
            String email = "notfound@mail.com";
            when(usersRepository.findByEmail(email)).thenReturn(null);

            assertThrows(UserNotFoundException.class, () -> usersService.getUserByEmail(email),
                    "Expected UserNotFoundException for non-existent email");
            verify(usersRepository).findByEmail(email);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when email is null or empty")
        void nullOrEmptyEmail_throwsException() {
            assertThrows(IllegalArgumentException.class, () -> usersService.getUserByEmail(null),
                    "Expected IllegalArgumentException for null email");
            assertThrows(IllegalArgumentException.class, () -> usersService.getUserByEmail(""),
                    "Expected IllegalArgumentException for empty email");
            verify(usersRepository, never()).findByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Successfully updates user")
        void successfulUpdate_updatesUser() {
            UsersDto updatedDto = UsersDto.builder()
                    .username("Alice Updated")
                    .email("alice.updated@mail.com")
                    .password("newPassword")
                    .build();
            Users existingUser = Users.builder()
                    .id(1L)
                    .username("Alice")
                    .email("alice@mail.com")
                    .password("hashedPassword")
                    .build();

            when(usersRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(usersRepository.save(any(Users.class))).thenReturn(existingUser);

            usersService.updateUser(1L, updatedDto);

            verify(usersRepository).findById(1L);
            verify(usersRepository).save(existingUser);
            assertEquals("Alice Updated", existingUser.getUsername());
            assertEquals("alice.updated@mail.com", existingUser.getEmail());
            assertEquals("newPassword", existingUser.getPassword());
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
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when UsersDto is null")
        void nullUsersDto_throwsException() {
            assertThrows(IllegalArgumentException.class, () -> usersService.updateUser(1L, null),
                    "Expected IllegalArgumentException for null UsersDto");
            verify(usersRepository, never()).findById(anyLong());
            verify(usersRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is invalid")
        void invalidId_throwsException() {
            UsersDto updatedDto = UsersDto.builder()
                    .username("Alice Updated")
                    .email("alice.updated@mail.com")
                    .build();

            assertThrows(IllegalArgumentException.class, () -> usersService.updateUser(0L, updatedDto),
                    "Expected IllegalArgumentException for invalid ID");
            assertThrows(IllegalArgumentException.class, () -> usersService.updateUser(null, updatedDto),
                    "Expected IllegalArgumentException for null ID");
            verify(usersRepository, never()).findById(anyLong());
            verify(usersRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Successfully deletes user")
        void successfulDelete_deletesUser() {
            when(usersRepository.existsById(1L)).thenReturn(true);
            doNothing().when(usersRepository).deleteById(1L);

            usersService.deleteUser(1L);

            verify(usersRepository).existsById(1L);
            verify(usersRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Throws UserNotFoundException when user not found")
        void userNotFound_throwsException() {
            when(usersRepository.existsById(1L)).thenReturn(false);

            assertThrows(UserNotFoundException.class, () -> usersService.deleteUser(1L),
                    "Expected UserNotFoundException for non-existent user");
            verify(usersRepository).existsById(1L);
            verify(usersRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when ID is invalid")
        void invalidId_throwsException() {
            assertThrows(IllegalArgumentException.class, () -> usersService.deleteUser(0L),
                    "Expected IllegalArgumentException for invalid ID");
            assertThrows(IllegalArgumentException.class, () -> usersService.deleteUser(null),
                    "Expected IllegalArgumentException for null ID");
            verify(usersRepository, never()).existsById(anyLong());
            verify(usersRepository, never()).deleteById(anyLong());
        }
    }
}