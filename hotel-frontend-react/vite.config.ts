import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

// In dev, proxy API + actuator to the running backend so the SPA talks to the
// same /api/v1 contract the old static frontend used.
//   - Backend via `./gradlew :bootstrap:bootRun`  → http://localhost:8080
//   - Full stack via `docker compose up` (nginx)  → http://localhost
// Override with VITE_API_TARGET in a .env file.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const target = env.VITE_API_TARGET || "http://localhost:8080";

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        "/api": { target, changeOrigin: true },
        "/actuator": { target, changeOrigin: true },
      },
    },
  };
});
