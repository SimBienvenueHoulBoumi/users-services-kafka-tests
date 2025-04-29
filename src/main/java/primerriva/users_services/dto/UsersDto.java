package primerriva.users_services.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UsersDto {
    private String username;
    private String email;
    private String password;
}


