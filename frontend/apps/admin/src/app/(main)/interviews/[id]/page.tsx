import { InterviewDetailClient } from "./interview-detail-client";

export function generateStaticParams() {
  return [{ id: "_" }];
}

export default function InterviewDetailPage() {
  return <InterviewDetailClient />;
}
