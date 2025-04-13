package primerriva.users_services.serviceImpl;

import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;
import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.exceptions.UserAlreadyExistsException;
import primerriva.users_services.exceptions.UserNotFoundException;
import primerriva.users_services.models.Users;
import primerriva.users_services.repositories.UsersRepository;
import primerriva.users_services.services.UsersService;

@Service
@AllArgsConstructor
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;

    /**
     * Retrieves a user by their email.
     * 
     * @param email the email of the user to retrieve
     * @return the user associated with the provided email
     * @throws IllegalArgumentException if the email is null or empty
     * @throws UserNotFoundException if no user is found with the given email
     */
    @Override
    public Users getUserByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }

        Users user = usersRepository.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException(email);
        }
        return user;
    }

    /**
     * Creates a new user with the provided details.
     * 
     * @param user the details of the user to create
     * @return the created user
     * @throws IllegalArgumentException if the user is null
     * @throws UserAlreadyExistsException if a user with the same email already exists
     */
    @Override
    public Users createUser(UsersDto user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        if (usersRepository.findByEmail(user.getEmail()) != null) {
            throw new UserAlreadyExistsException(user.getEmail());
        }

        Users newUser = new Users();
        newUser.setName(user.getName());
        newUser.setEmail(user.getEmail());

        return usersRepository.save(newUser);
    }

    /**
     * Updates the user with the given ID.
     * 
     * @param id the ID of the user to update
     * @param user the updated details of the user
     * @return the updated user
     * @throws UserNotFoundException if no user is found with the given ID
     */
    @Override
    public Users updateUser(Long id, UsersDto user) {
        Users existingUser = usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());

        return usersRepository.save(existingUser);
    }

    /**
     * Deletes the user with the given ID.
     * 
     * @param id the ID of the user to delete
     * @throws UserNotFoundException if no user is found with the given ID
     */
    @Override
    public void deleteUser(Long id) {
        if (!usersRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        usersRepository.deleteById(id);
    }
}
