package com.voyageops.app.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private Integer age;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}