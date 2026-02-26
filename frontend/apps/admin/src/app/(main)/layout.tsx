import { TopNav } from "@/components/admin/top-nav";

export default function MainLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col">
      <TopNav />
      <main className="flex-1 p-8">{children}</main>
    </div>
  );
}
