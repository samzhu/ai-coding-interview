import type { NextConfig } from "next";

const isExport = process.env.NEXT_EXPORT === "1";

const nextConfig: NextConfig = {
  transpilePackages: ["@interview/shared"],
  output: isExport ? "export" : undefined,
  trailingSlash: isExport,
  // 設計說明：monitor 頁面或 AI 功能的 SSE 串流可能持續 2-5 分鐘，
  // 對齊 candidate app 設定，防止 proxy 提前斷線。
  experimental: {
    proxyTimeout: 300000,
  },
  ...(!isExport && {
    async rewrites() {
      return [
        // API proxy：開發時將 /api/v1/* 代理至後端 Spring Boot
        {
          source: "/api/v1/:path*",
          destination: "http://localhost:8080/api/v1/:path*",
        },
      ];
    },
  }),
};

export default nextConfig;
