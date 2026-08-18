package com.voyageops.app.user.domain;

import com.voyageops.app.common.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * Pure rules around trip membership roles.
 * <p>
 * The {@link MemberRole#OWNER} role is reserved: it can never be assigned,
 * changed or removed through member management. Only the trip creator owns it.
 */
@Component
public class MembershipPolicy {

    public void validateAssignableRole(MemberRole role) {
        if (role == null) {
            throw new BusinessException("Role is required");
        }
        if (role == MemberRole.OWNER) {
            throw new BusinessException("OWNER role cannot be assigned via member management");
        }
    }

    public void validateRoleChange(MemberRole current, MemberRole target) {
        if (current == MemberRole.OWNER) {
            throw new BusinessException("The trip owner's role cannot be changed");
        }
        validateAssignableRole(target);
    }

    public void validateRemoval(MemberRole role) {
        if (role == MemberRole.OWNER) {
            throw new BusinessException("The trip owner cannot be removed");
        }
    }
}
