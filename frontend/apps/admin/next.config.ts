import type { NextConfig } from "next";

const isExport = process.env.NEXT_EXPORT === "1";

const nextConfig: NextConfig = {
  transpilePackages: ["@interview/shared"],
  output: isExport ? "export" : undefined,
  trailingSlash: isExport,
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
