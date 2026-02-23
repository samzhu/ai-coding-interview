import { MonitorClient } from "./monitor-client";

export function generateStaticParams() {
  return [{ id: '_' }];
}

export default function MonitorPage() {
  return <MonitorClient />;
}
