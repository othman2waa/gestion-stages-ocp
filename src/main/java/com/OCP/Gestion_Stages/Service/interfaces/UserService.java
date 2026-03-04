package com.OCP.Gestion_Stages.Service.interfaces;

import com.OCP.Gestion_Stages.domain.dto.user.UserRequest;
import com.OCP.Gestion_Stages.domain.dto.user.UserResponse;
import java.util.List;

public interface UserService {
    List<UserResponse> findAll();
    UserResponse findById(Long id);
    UserResponse update(Long id, UserRequest request);
    void delete(Long id);
    UserResponse toggleActif(Long id);
    UserResponse getCurrentUser();
}