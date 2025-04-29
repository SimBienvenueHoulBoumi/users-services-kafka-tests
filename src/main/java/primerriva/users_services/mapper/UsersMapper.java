package primerriva.users_services.mapper;

import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.models.Users;

public class UsersMapper {

    public static Users toEntity(UsersDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("UsersDto must not be null");
        }
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username must not be null or empty");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }
        return Users.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .build();
    }
}