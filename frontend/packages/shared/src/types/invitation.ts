// Invitation module types — mirrors backend InvitationResponse

export interface InvitationResponse {
  id: string;
  interviewId: string;
  token: string;
  expiresAt: string;
  createdAt: string;
}
