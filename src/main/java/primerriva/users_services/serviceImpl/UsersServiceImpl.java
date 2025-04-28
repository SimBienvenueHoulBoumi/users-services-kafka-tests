package primerriva.users_services.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
                .name(user.getName())
                .email(user.getEmail())
                .action("GET_ONE")
                .timestamp(LocalDateTime.now())
                .build();
        sendToKafka(USER_GET_ONE_TOPIC, eventDto);

        return user;
    }

    @Override
    public void createUser(UsersDto userDto) {
        if (userDto == null || userDto.getName() == null || userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new IllegalArgumentException("User name and email must not be null or empty");
        }

        if (!userDto.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (usersRepository.findByEmail(userDto.getEmail()) != null) {
            throw new UserAlreadyExistsException(userDto.getEmail());
        }

        Users newUser = UsersMapper.toEntity(userDto);
        newUser = usersRepository.save(newUser);

        UserEventDto eventDto = UserEventDto.builder()
                .id(newUser.getId())
                .name(userDto.getName())
                .email(userDto.getEmail())
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

        Users updatedUser = UsersMapper.toEntity(userDto);
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());

        usersRepository.save(existingUser);

        UserEventDto eventDto = UserEventDto.builder()
                .id(id)
                .name(userDto.getName())
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

    private void sendToKafka(String topic, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            logger.debug("Envoi du message au topic {}: {}", topic, json);
            kafkaTemplate.send(topic, json);
        } catch (JsonProcessingException e) {
            logger.error("Erreur lors de l'envoi du message au topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi du message Kafka", e);
        }
    }
}