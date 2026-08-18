package com.voyageops.app.user.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.user.domain.User;
import com.voyageops.app.user.infrastructure.UserRepository;
import com.voyageops.app.user.web.dto.CreateUserRequest;
import com.voyageops.app.user.web.dto.UpdateUserRequest;
import com.voyageops.app.user.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void createUser_persistsNormalizedEmail() {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("Alice")
                .age(30)
                .email("  Alice@Example.COM ")
                .build();
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        UserResponse response = userService.createUser(request);

        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getAge()).isEqualTo(30);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).existsByEmail("alice@example.com");
    }

    @Test
    void createUser_rejectsDuplicateEmail() {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .build();
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void getUserById_returnsMappedUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .age(30)
                .email("alice@example.com")
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(user.getId());

        assertThat(response.getId()).isEqualTo(user.getId());
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void getUserById_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateUser_appliesChanges() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .age(30)
                .email("alice@example.com")
                .build();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .name("Alicia")
                .age(31)
                .email("alicia@example.com")
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("alicia@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateUser(user.getId(), request);

        assertThat(response.getName()).isEqualTo("Alicia");
        assertThat(response.getAge()).isEqualTo(31);
        assertThat(response.getEmail()).isEqualTo("alicia@example.com");
    }

    @Test
    void updateUser_skipsUniquenessCheckWhenEmailUnchanged() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@example.com")
                .build();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .name("Alice Updated")
                .email("alice@example.com")
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateUser(user.getId(), request);

        assertThat(response.getName()).isEqualTo("Alice Updated");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void updateUser_rejectsEmailTakenByAnotherUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .email("alice@example.com")
                .build();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("bob@example.com")
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(user.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");
    }
}
