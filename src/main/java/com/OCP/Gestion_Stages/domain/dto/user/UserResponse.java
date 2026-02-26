package com.OCP.Gestion_Stages.domain.dto.user;


import com.OCP.Gestion_Stages.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private Boolean actif;
    private LocalDateTime createdAt;
}