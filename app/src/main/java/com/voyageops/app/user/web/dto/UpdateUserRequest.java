package com.voyageops.app.user.web.dto;

import jakarta.validation.constraints.Email;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    private String name;

    private Integer age;

    @Email(message = "Email must be valid")
    private String email;
}