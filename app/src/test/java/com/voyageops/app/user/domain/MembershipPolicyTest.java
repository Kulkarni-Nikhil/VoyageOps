package com.voyageops.app.user.domain;

import com.voyageops.app.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipPolicyTest {

    private final MembershipPolicy policy = new MembershipPolicy();

    @Test
    void validateAssignableRole_acceptsMember() {
        assertThatCode(() -> policy.validateAssignableRole(MemberRole.MEMBER))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAssignableRole_acceptsEditor() {
        assertThatCode(() -> policy.validateAssignableRole(MemberRole.EDITOR))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAssignableRole_rejectsOwner() {
        assertThatThrownBy(() -> policy.validateAssignableRole(MemberRole.OWNER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OWNER");
    }

    @Test
    void validateAssignableRole_rejectsNull() {
        assertThatThrownBy(() -> policy.validateAssignableRole(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Role is required");
    }

    @Test
    void validateRoleChange_acceptsMemberToEditor() {
        assertThatCode(() -> policy.validateRoleChange(MemberRole.MEMBER, MemberRole.EDITOR))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRoleChange_rejectsChangingOwnerRole() {
        assertThatThrownBy(() -> policy.validateRoleChange(MemberRole.OWNER, MemberRole.EDITOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("owner's role");
    }

    @Test
    void validateRoleChange_rejectsPromotionToOwner() {
        assertThatThrownBy(() -> policy.validateRoleChange(MemberRole.MEMBER, MemberRole.OWNER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OWNER");
    }

    @Test
    void validateRemoval_acceptsMember() {
        assertThatCode(() -> policy.validateRemoval(MemberRole.MEMBER))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRemoval_rejectsOwner() {
        assertThatThrownBy(() -> policy.validateRemoval(MemberRole.OWNER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("owner");
    }
}
