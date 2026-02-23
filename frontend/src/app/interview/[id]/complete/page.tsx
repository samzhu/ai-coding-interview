import { CompleteClient } from "./complete-client";

export function generateStaticParams() {
  return [{ id: '_' }];
}

export default function CompletePage() {
  return <CompleteClient />;
}
