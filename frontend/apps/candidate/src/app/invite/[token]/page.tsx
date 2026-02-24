import { InviteClient } from "./invite-client";

// Generate empty params — dynamic routes handled by nginx SPA fallback
export async function generateStaticParams() {
  return [{ token: '_' }];
}

export default function InvitePage() {
  return <InviteClient />;
}
