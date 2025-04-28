package primerriva.users_services.mapper;

import primerriva.users_services.dto.UsersDto;
import primerriva.users_services.models.Users;

public class UsersMapper {
    public static Users toEntity(UsersDto dto) {
        if (dto == null) return null;

        return Users.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .build();
    }
    public static UsersDto toDto(Users entity) {
        if (entity == null) return null;

        return UsersDto.builder()
                .name(entity.getName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .build();
    }
    public static UsersDto toDtoWithId(Users entity) {
        if (entity == null) return null;

        return UsersDto.builder()
                .name(entity.getName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .build();
    }
}
