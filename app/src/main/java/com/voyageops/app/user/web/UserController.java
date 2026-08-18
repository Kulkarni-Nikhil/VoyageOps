package com.voyageops.app.user.web;

import com.voyageops.app.common.exception.UnauthorizedException;
import com.voyageops.app.common.web.CurrentUserProvider;
import com.voyageops.app.user.application.UserService;
import com.voyageops.app.user.web.dto.CreateUserRequest;
import com.voyageops.app.user.web.dto.UpdateUserRequest;
import com.voyageops.app.user.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/me")
    public UserResponse currentUser() {
        return userService.getUserById(requireActor());
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    private UUID requireActor() {
        UUID id = currentUserProvider.currentUserId();
        if (id == null) {
            throw new UnauthorizedException("Missing X-User-Id header");
        }
        return id;
    }
}
