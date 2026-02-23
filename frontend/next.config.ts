import type { NextConfig } from "next";

// NEXT_EXPORT=1 is set by Gradle buildFrontend task for production static export.
// In development (npm run dev / npm run build locally), API proxy routes work normally.
const isExport = process.env.NEXT_EXPORT === "1";

const nextConfig: NextConfig = {
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
