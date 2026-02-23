package com.interview.invitation.interfaces.rest;

import com.interview.invitation.application.InvitationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
class InvitationController {

    private final InvitationService service;

    InvitationController(InvitationService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/interviews/{interviewId}/invitation")
    @ResponseStatus(HttpStatus.CREATED)
    InvitationResponse createInvitation(@PathVariable UUID interviewId) {
        return InvitationResponse.from(service.createOrGetInvitation(interviewId));
    }

    @GetMapping("/api/v1/invitations/{token}")
    InvitationResponse getInvitation(@PathVariable String token) {
        return InvitationResponse.from(service.getByToken(token));
    }

    @PostMapping("/api/v1/invitations/{token}/join")
    InvitationResponse joinInterview(@PathVariable String token) {
        return InvitationResponse.from(service.joinByToken(token));
    }
}
