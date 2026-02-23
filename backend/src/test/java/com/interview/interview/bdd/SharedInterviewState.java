package com.interview.interview.bdd;

import com.interview.interview.application.CheckpointProgressService.CheckpointView;
import com.interview.interview.domain.Interview;
import com.interview.invitation.domain.Invitation;
import org.springframework.stereotype.Component;

/**
 * 跨 step definition 類別共享面試測試狀態。
 * Cucumber Spring 共用單一 Spring context，使用 @Component 共享狀態。
 */
@Component
public class SharedInterviewState {

    private Interview currentInterview;
    private Invitation currentInvitation;
    private CheckpointView currentCheckpointView;

    public Interview getCurrentInterview() { return currentInterview; }
    public void setCurrentInterview(Interview interview) { this.currentInterview = interview; }

    public Invitation getCurrentInvitation() { return currentInvitation; }
    public void setCurrentInvitation(Invitation invitation) { this.currentInvitation = invitation; }

    public CheckpointView getCurrentCheckpointView() { return currentCheckpointView; }
    public void setCurrentCheckpointView(CheckpointView view) { this.currentCheckpointView = view; }

    public void reset() {
        currentInterview = null;
        currentInvitation = null;
        currentCheckpointView = null;
    }
}
