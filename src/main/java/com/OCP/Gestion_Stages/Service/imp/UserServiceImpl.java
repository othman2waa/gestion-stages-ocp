package com.OCP.Gestion_Stages.Service.imp;

import com.OCP.Gestion_Stages.Repository.UserRepository;
import com.OCP.Gestion_Stages.Service.interfaces.UserService;
import com.OCP.Gestion_Stages.domain.dto.user.UserRequest;
import com.OCP.Gestion_Stages.domain.dto.user.UserResponse;
import com.OCP.Gestion_Stages.domain.model.User;
import com.OCP.Gestion_Stages.exeptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public UserResponse findById(Long id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable : " + id)));
    }

    @Override
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable : " + id));
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank())
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (request.getRole() != null)
            user.setRole(request.getRole());
        return toResponse(userRepository.save(user));
    }

    @Override
    public void delete(Long id) {
        if (!userRepository.existsById(id))
            throw new ResourceNotFoundException("User introuvable : " + id);
        userRepository.deleteById(id);
    }

    @Override
    public UserResponse toggleActif(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable : " + id));
        user.setActif(!user.getActif());
        return toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return toResponse(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User introuvable")));
    }

    private UserResponse toResponse(User u) {
        UserResponse response = new UserResponse();
        response.setId(u.getId());
        response.setUsername(u.getUsername());
        response.setEmail(u.getEmail());
        response.setRole(u.getRole());
        response.setActif(u.getActif());
        response.setCreatedAt(u.getCreatedAt());
        return response;
    }
}