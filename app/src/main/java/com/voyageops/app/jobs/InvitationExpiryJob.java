package com.voyageops.app.jobs;

import com.voyageops.app.user.application.InvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marks invitations whose 48h validity has elapsed as {@code EXPIRED}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationExpiryJob {

    private final InvitationService invitationService;

    @Scheduled(cron = "0 0 * * * *")
    public void expireOverdueInvitations() {
        int expired = invitationService.expireOverdueInvitations();
        log.info("Invitation expiry run: {} invitation(s) expired", expired);
    }
}