"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@interview/shared/lib/utils";

const navLinks = [
  { href: "/", label: "Dashboard" },
  { href: "/interviews", label: "Interviews" },
  { href: "/questions", label: "Questions" },
];

export function TopNav() {
  const pathname = usePathname();

  return (
    <nav className="h-14 bg-card border-b border-border flex items-center px-6 gap-8 shrink-0">
      <span className="text-foreground font-semibold text-sm">AI 面試平台</span>
      <div className="flex items-center h-full gap-1">
        {navLinks.map((link) => {
          const isActive =
            link.href === "/" ? pathname === "/" : pathname.startsWith(link.href);
          return (
            <Link
              key={link.href}
              href={link.href}
              className={cn(
                "h-full px-3 flex items-center text-sm border-b-2 transition-colors",
                isActive
                  ? "text-primary font-medium border-primary"
                  : "text-muted-foreground hover:text-foreground border-transparent"
              )}
            >
              {link.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
