package com.OCP.Gestion_Stages.domain.dto.auth;


import com.OCP.Gestion_Stages.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String email;
    private UserRole role;
    private Long userId;
}