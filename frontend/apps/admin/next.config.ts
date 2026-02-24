import type { NextConfig } from "next";

const isExport = process.env.NEXT_EXPORT === "1";

const nextConfig: NextConfig = {
  transpilePackages: ["@interview/shared"],
  output: isExport ? "export" : undefined,
  trailingSlash: isExport,
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
