import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${process.env.BACKEND_URL || "http://localhost:8080/api"}/:path*`,
      },
    ];
  },
  experimental: {
    optimizePackageImports: ["amis", "amis-editor", "@xyflow/react"],
  },
};

export default nextConfig;
