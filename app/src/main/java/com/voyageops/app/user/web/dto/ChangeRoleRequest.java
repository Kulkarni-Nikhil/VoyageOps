package com.voyageops.app.user.web.dto;

import com.voyageops.app.user.domain.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeRoleRequest {

    @NotNull(message = "role is required")
    private MemberRole role;
}
