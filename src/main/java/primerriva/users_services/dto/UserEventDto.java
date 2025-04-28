package primerriva.users_services.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserEventDto {
    private Long id;
    private String name;
    private String email;
    private String password;
    private String action; 
    private LocalDateTime timestamp;
}