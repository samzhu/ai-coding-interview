package com.interview.invitation.interfaces.rest;

import com.interview.invitation.application.InvitationService;
import com.interview.invitation.domain.Invitation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.interview.WebMvcTestSliceConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({InvitationController.class, InvitationExceptionHandler.class})
@Import(WebMvcTestSliceConfig.class)
@DisplayName("InvitationController REST API 測試")
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationService service;

    @Test
    @DisplayName("POST /api/v1/interviews/{interviewId}/invitation 應回傳 201 和邀請 token")
    void shouldCreateInvitationAndReturn201() throws Exception {
        UUID interviewId = UUID.randomUUID();
        Invitation invitation = Invitation.create(interviewId);
        when(service.createOrGetInvitation(interviewId)).thenReturn(invitation);

        mockMvc.perform(post("/api/v1/interviews/{interviewId}/invitation", interviewId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.interviewId").value(interviewId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/invitations/{token} 應回傳邀請資訊")
    void shouldGetInvitationByToken() throws Exception {
        UUID interviewId = UUID.randomUUID();
        Invitation invitation = Invitation.create(interviewId);
        when(service.getByToken(invitation.getToken())).thenReturn(invitation);

        mockMvc.perform(get("/api/v1/invitations/{token}", invitation.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewId").value(interviewId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/invitations/{token} token 不存在時回傳 404")
    void shouldReturn404ForInvalidToken() throws Exception {
        when(service.getByToken("invalid")).thenThrow(new IllegalArgumentException("not found"));

        mockMvc.perform(get("/api/v1/invitations/invalid"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/invitations/{token}/join 應觸發面試開始")
    void shouldJoinInterviewByToken() throws Exception {
        UUID interviewId = UUID.randomUUID();
        Invitation invitation = Invitation.create(interviewId);
        when(service.joinByToken(invitation.getToken())).thenReturn(invitation);

        mockMvc.perform(post("/api/v1/invitations/{token}/join", invitation.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewId").value(interviewId.toString()));
    }
}
