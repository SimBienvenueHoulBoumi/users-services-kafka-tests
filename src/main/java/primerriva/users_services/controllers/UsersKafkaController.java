package primerriva.users_services.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import primerriva.users_services.config.KafkaUserRequestHandler;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class UsersKafkaController {

    private final KafkaUserRequestHandler requestHandler;

    @KafkaListener(topics = "request-create-user", groupId = "users-service-group")
    public void handleCreateUser(@Payload String message) {
        requestHandler.handleUserRequest(message, "createUser");
    }

    @KafkaListener(topics = "request-update-user", groupId = "users-service-group")
    public void handleUpdateUser(@Payload String message) {
        requestHandler.handleUserRequest(message, "updateUser");
    }

    @KafkaListener(topics = "request-get-user-by-email", groupId = "users-service-group")
    public void handleGetUserByEmail(@Payload String message) {
        requestHandler.handleUserRequest(message, "getUserByEmail");
    }

    @KafkaListener(topics = "request-delete-user", groupId = "users-service-group")
    public void handleDeleteUser(@Payload String message) {
        requestHandler.handleUserRequest(message, "deleteUser");
    }
}
