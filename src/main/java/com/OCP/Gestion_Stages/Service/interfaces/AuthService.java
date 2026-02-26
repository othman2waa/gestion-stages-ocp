package com.OCP.Gestion_Stages.Service.interfaces;


import com.OCP.Gestion_Stages.domain.dto.auth.AuthResponse;
import com.OCP.Gestion_Stages.domain.dto.auth.LoginRequest;
import com.OCP.Gestion_Stages.domain.dto.user.UserRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(UserRequest request);
}