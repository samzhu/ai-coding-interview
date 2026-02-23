package com.interview.invitation.infrastructure.persistence;

import com.interview.invitation.domain.Invitation;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends ListCrudRepository<Invitation, UUID> {

    @Query("SELECT * FROM interview_invitations WHERE interview_id = :interviewId")
    Optional<Invitation> findByInterviewId(@Param("interviewId") UUID interviewId);

    @Query("SELECT * FROM interview_invitations WHERE token = :token")
    Optional<Invitation> findByToken(@Param("token") String token);
}
