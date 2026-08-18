package com.voyageops.app.user.web;

import com.voyageops.app.common.web.CurrentUserProvider;
import com.voyageops.app.user.application.TripMemberService;
import com.voyageops.app.user.web.dto.ChangeRoleRequest;
import com.voyageops.app.user.web.dto.TripMemberRequest;
import com.voyageops.app.user.web.dto.TripMemberResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Trip Members")
public class TripMemberController {

    private final TripMemberService tripMemberService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/trips/{tripId}/members")
    public List<TripMemberResponse> listMembers(@PathVariable Long tripId) {
        return tripMemberService.listMembers(tripId, actor());
    }

    @PostMapping("/trips/{tripId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public TripMemberResponse addMember(@PathVariable Long tripId,
                                        @Valid @RequestBody TripMemberRequest request) {
        return tripMemberService.addMember(tripId, actor(), request);
    }

    @PatchMapping("/trips/{tripId}/members/{userId}")
    public TripMemberResponse changeRole(@PathVariable Long tripId,
                                         @PathVariable UUID userId,
                                         @Valid @RequestBody ChangeRoleRequest request) {
        return tripMemberService.changeRole(tripId, actor(), userId, request.getRole());
    }

    @DeleteMapping("/trips/{tripId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long tripId, @PathVariable UUID userId) {
        tripMemberService.removeMember(tripId, actor(), userId);
    }

    private UUID actor() {
        return currentUserProvider.currentUserId();
    }
}
