package com.voyageops.app.user.web;

import com.voyageops.app.common.web.CurrentUserProvider;
import com.voyageops.app.user.application.InvitationService;
import com.voyageops.app.user.web.dto.InvitationRequest;
import com.voyageops.app.user.web.dto.InvitationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/trips/{tripId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse create(@PathVariable Long tripId,
                                     @Valid @RequestBody InvitationRequest request) {
        return invitationService.createInvitation(tripId, actor(), request);
    }

    @GetMapping("/invitations/{token}")
    public InvitationResponse get(@PathVariable String token) {
        return invitationService.getInvitation(token);
    }

    @PostMapping("/invitations/{token}/accept")
    public InvitationResponse accept(@PathVariable String token) {
        return invitationService.acceptInvitation(token, actor());
    }

    @PostMapping("/invitations/{token}/decline")
    public InvitationResponse decline(@PathVariable String token) {
        return invitationService.declineInvitation(token, actor());
    }

    private UUID actor() {
        return currentUserProvider.currentUserId();
    }
}
