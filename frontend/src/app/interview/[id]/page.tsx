import { InterviewClient } from "./interview-client";

// Generate empty params — dynamic routes handled by Spring Boot SPA fallback
export async function generateStaticParams() {
  return [{ id: '_' }];
}

export default function InterviewPage() {
  return <InterviewClient />;
}
