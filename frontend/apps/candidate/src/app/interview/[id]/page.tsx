import { InterviewClient } from "./interview-client";

// Generate empty params — dynamic routes handled by nginx SPA fallback
export async function generateStaticParams() {
  return [{ id: '_' }];
}

export default function InterviewPage() {
  return <InterviewClient />;
}
