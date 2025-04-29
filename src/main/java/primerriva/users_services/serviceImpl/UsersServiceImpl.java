package primerriva.users_services.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.exceptions.UserAlreadyExistsException;
import primerriva.users_services.exceptions.UserNotFoundException;
import primerriva.users_services.mapper.UsersMapper;
import primerriva.users_services.models.Users;
import primerriva.users_services.repositories.UsersRepository;
import primerriva.users_services.services.UsersService;

@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {

    private static final Logger logger = LoggerFactory.getLogger(UsersServiceImpl.class);

    private final UsersRepository usersRepository;

    @Override
    public Users getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            logger.warn("Email must not be null or empty");
            throw new IllegalArgumentException("Email must not be null or empty");
        }
        Users user = usersRepository.findByEmail(email);
        if (user == null) {
            logger.warn("User not found for email: {}", email);
            throw new UserNotFoundException(email);
        }
        logger.debug("Retrieved user with email: {}", email);
        return user;
    }

    @Override
    public void createUser(UsersDto userDto) {
        if (userDto == null) {
            logger.warn("User DTO must not be null");
            throw new IllegalArgumentException("User DTO must not be null");
        }
        if (usersRepository.findByEmail(userDto.getEmail()) != null) {
            logger.warn("User already exists with email: {}", userDto.getEmail());
            throw new UserAlreadyExistsException(userDto.getEmail());
        }
        Users user = UsersMapper.toEntity(userDto);
        usersRepository.save(user);
        logger.info("Created user with email: {}", userDto.getEmail());
    }

    @Override
    public void updateUser(Long id, UsersDto userDto) {
        if (id == null || id <= 0) {
            logger.warn("ID must be a positive number");
            throw new IllegalArgumentException("ID must be a positive number");
        }
        if (userDto == null) {
            logger.warn("User DTO must not be null");
            throw new IllegalArgumentException("User DTO must not be null");
        }
        Users existingUser = usersRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found for id: {}", id);
                    return new UserNotFoundException(id);
                });
        Users updatedUser = UsersMapper.toEntity(userDto);
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPassword(updatedUser.getPassword());
        usersRepository.save(existingUser);
        logger.info("Updated user with id: {}", id);
    }

    @Override
    public void deleteUser(Long id) {
        if (id == null || id <= 0) {
            logger.warn("ID must be a positive number");
            throw new IllegalArgumentException("ID must be a positive number");
        }
        if (!usersRepository.existsById(id)) {
            logger.warn("User not found for id: {}", id);
            throw new UserNotFoundException(id);
        }
        usersRepository.deleteById(id);
        logger.info("Deleted user with id: {}", id);
    }
}