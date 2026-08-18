package com.voyageops.app.jobs;

import com.voyageops.app.user.application.InvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationExpiryJobTest {

    @Mock
    private InvitationService invitationService;

    private InvitationExpiryJob job;

    @BeforeEach
    void setUp() {
        job = new InvitationExpiryJob(invitationService);
    }

    @Test
    void expireOverdueInvitations_delegatesToService() {
        when(invitationService.expireOverdueInvitations()).thenReturn(3);

        job.expireOverdueInvitations();

        verify(invitationService).expireOverdueInvitations();
    }
}
