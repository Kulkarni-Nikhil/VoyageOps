package com.voyageops.app.user.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.user.domain.User;
import com.voyageops.app.user.infrastructure.UserRepository;
import com.voyageops.app.user.web.dto.CreateUserRequest;
import com.voyageops.app.user.web.dto.UpdateUserRequest;
import com.voyageops.app.user.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class


UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return toResponse(getUser(id));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Email is already registered");
        }
        User user = User.builder()
                .name(request.getName())
                .age(request.getAge())
                .email(email)
                .build();
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = getUser(id);
        if (request.getEmail() != null) {
            String email = normalizeEmail(request.getEmail());
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new BusinessException(HttpStatus.CONFLICT, "Email is already registered");
            }
            user.setEmail(email);
        }
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .age(user.getAge())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
