package primerriva.users_services.serviceImpl;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.exceptions.UserAlreadyExistsException;
import primerriva.users_services.exceptions.UserNotFoundException;
import primerriva.users_services.models.Users;
import primerriva.users_services.repositories.UsersRepository;
import primerriva.users_services.services.UsersService;

/**
 * Implémentation de l'interface UsersService utilisant Kafka pour la communication asynchrone.
 */
@Service
@AllArgsConstructor
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String USER_GET_ONE_TOPIC = "user-get-one-topic";
    private static final String USER_CREATED_TOPIC = "user-created-topic";
    private static final String USER_UPDATED_TOPIC = "user-updated-topic";
    private static final String USER_DELETED_TOPIC = "user-deleted-topic";

    @Override
    public void getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }

        Users user = usersRepository.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException(email);
        }

        UsersDto userDto = new UsersDto(user.getName(), user.getEmail());
        sendToKafka(USER_GET_ONE_TOPIC, userDto);
    }

    @Override
    public void createUser(UsersDto userDto) {
        if (userDto == null || userDto.getName() == null || userDto.getEmail() == null) {
            throw new IllegalArgumentException("User name and email must not be null");
        }

        if (usersRepository.findByEmail(userDto.getEmail()) != null) {
            throw new UserAlreadyExistsException(userDto.getEmail());
        }

        Users newUser = new Users();
        newUser.setName(userDto.getName());
        newUser.setEmail(userDto.getEmail());
        usersRepository.save(newUser);

        sendToKafka(USER_CREATED_TOPIC, userDto);
    }

    @Override
    public void updateUser(Long id, UsersDto userDto) {
        if (userDto == null) {
            throw new IllegalArgumentException("User DTO must not be null");
        }

        Users existingUser = usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        existingUser.setName(userDto.getName());
        existingUser.setEmail(userDto.getEmail());

        usersRepository.save(existingUser);

        sendToKafka(USER_UPDATED_TOPIC, userDto);
    }

    @Override
    public void deleteUser(Long id) {
        Users userToDelete = usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        usersRepository.deleteById(id);

        // on envoie un objet pour rester cohérent avec les autres événements
        DeletedUserEvent deletedEvent = new DeletedUserEvent(id, userToDelete.getEmail(), "DELETED");
        sendToKafka(USER_DELETED_TOPIC, deletedEvent);
    }

    /**
     * Envoie un objet ou une chaîne déjà serialisée à Kafka (en JSON).
     *
     * @param topic le nom du topic Kafka
     * @param data l'objet à envoyer (ou une String JSON)
     */
    private void sendToKafka(String topic, Object data) {
        try {
            String json = (data instanceof String) ? (String) data : objectMapper.writeValueAsString(data);
            kafkaTemplate.send(topic, json);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi du message Kafka", e);
        }
    }

    /**
     * Représente l’événement envoyé lors d’une suppression d’utilisateur.
     */
    private record DeletedUserEvent(Long id, String email, String action) {}
}
