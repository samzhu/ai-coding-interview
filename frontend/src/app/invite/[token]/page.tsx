import { InviteClient } from "./invite-client";

// Generate empty params — dynamic routes handled by Spring Boot SPA fallback
export async function generateStaticParams() {
  return [{ token: '_' }];
}

export default function InvitePage() {
  return <InviteClient />;
}
