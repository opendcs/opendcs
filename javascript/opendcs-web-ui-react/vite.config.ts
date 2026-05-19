/// <reference types="vitest/config" />
import { resolve } from "path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import svgr from "vite-plugin-svgr";

// https://vite.dev/config/
import path from "node:path";
import { fileURLToPath } from "node:url";
import { storybookTest } from "@storybook/addon-vitest/vitest-plugin";
import { playwright } from "@vitest/browser-playwright";
const dirname =
  typeof __dirname === "undefined"
    ? path.dirname(fileURLToPath(import.meta.url))
    : __dirname;

// More info at: https://storybook.js.org/docs/next/writing-tests/integrations/vitest-addon
export default defineConfig({
  plugins: [react(), svgr()],
  optimizeDeps: {
    exclude: ["opendcs-api", "whatwg-fetch"],
    // Pre-bundle deps that would otherwise trigger a mid-test re-optimize.
    // react-query-devtools is statically imported by QueryProvider, so once
    // its first transitive dep loads, vite re-bundles and reloads every test
    // file — see the storybook retry comment below.
    include: [
      "react-dom/client",
      "react-router-dom",
      "@tanstack/react-query",
      "@tanstack/react-query-devtools",
    ],
  },
  server: {
    fs: {
      // Allow serving files from one level up the project root
      allow: [
        ".",
        resolve(
          __dirname,
          "../../java/api-clients/api-client-typescript/build/generated/openApi",
        ),
      ],
      // Or explicitly allow the path to your linked package
      // allow: [path.resolve(__dirname, '../path/to/your/linked-package')]
    },
    proxy: {
      // Proxy requests starting with '/api'
      "/odcsapi": {
        target: "http://localhost:7000", // The address of your backend server
        changeOrigin: true, // Needed for virtual hosted sites
      },
    },
  },
  resolve: {
    alias: {
      "opendcs-api": resolve(
        __dirname,
        "../../java/api-clients/api-client-typescript/build/generated/openApi",
      ),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        silenceDeprecations: [
          "import",
          "global-builtin",
          "color-functions",
          "if-function",
        ],
      },
    },
  },
  build: {
    commonjsOptions: {
      include: [/opendcs-api/, /node_modules/],
    },
  },
  test: {
    coverage: {
      enabled: true, // Ensure coverage is enabled
      provider: "istanbul", // Use 'istanbul' as the provider for LCOV output
      reporter: ["text", "lcov"], // Specify 'lcov' (and optionally 'text' for console summary)
    },
    projects: [
      {
        extends: true,
        plugins: [
          // The plugin will run tests for the stories defined in your Storybook config
          // See options at: https://storybook.js.org/docs/next/writing-tests/integrations/vitest-addon#storybooktest
          storybookTest({
            configDir: path.join(dirname, ".storybook"),
          }),
        ],
        test: {
          name: "storybook",
          // Retry once on failure. Storybook+vitest's browser-mode dep
          // optimizer occasionally loses a race when vite re-bundles deps
          // mid-load — a test file's import 404s with "Failed to fetch
          // dynamically imported module" using a now-invalid `?v=...` URL.
          // The cache (see .github/workflows/build.yml) covers the broad
          // case; this retry handles the residual single-file flake.
          retry: 1,
          browser: {
            enabled: true,
            headless: true,
            provider: playwright({}),
            instances: [
              { browser: "chromium" },
              //{browser: 'firefox'}
            ],
          },
          setupFiles: [".storybook/vitest.setup.ts"],
        },
      },
      {
        extends: true,
        test: {
          name: "unit",
          include: ["src/**/*.test.(ts|tsx|js|jsx)"],
        },
      },
    ],
  },
});
