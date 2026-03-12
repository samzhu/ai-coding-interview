import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { Toaster } from "@interview/shared/components/ui/sonner";
import { OidcProviderWrapper } from "@/components/auth/oidc-provider-wrapper";
import { AuthContextProvider } from "@/contexts/auth-context";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "AI 程式面試平台 — 管理後台",
  description: "面試官管理後台",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-TW" className="dark" style={{ colorScheme: "dark" }}>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        <OidcProviderWrapper>
          <AuthContextProvider>
            {children}
          </AuthContextProvider>
        </OidcProviderWrapper>
        <Toaster />
      </body>
    </html>
  );
}
