import type { NextConfig } from "next";

const isExport = process.env.NEXT_EXPORT === "1";

const nextConfig: NextConfig = {
  transpilePackages: ["@interview/shared"],
  output: isExport ? "export" : undefined,
  trailingSlash: isExport,
  // 設計說明：AI 聊天 SSE 串流含 tool calling（runCommand、editProposal 等），
  // 單次請求可能持續 2-5 分鐘。預設 proxyTimeout 為 30 秒，會導致 proxy 提前斷線，
  // 後端 StreamingResponseBody 執行緒收到 InterruptedException。
  // 300000ms = 5 分鐘，對齊後端 AiChatController latch.await(5, MINUTES) 的安全上限。
  experimental: {
    proxyTimeout: 300000,
  },
  ...(!isExport && {
    async rewrites() {
      return [
        {
          source: "/api/v1/:path*",
          destination: "http://localhost:8080/api/v1/:path*",
        },
      ];
    },
  }),
};

export default nextConfig;
