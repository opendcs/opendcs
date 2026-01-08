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
  typeof __dirname !== "undefined"
    ? __dirname
    : path.dirname(fileURLToPath(import.meta.url));

// More info at: https://storybook.js.org/docs/next/writing-tests/integrations/vitest-addon
export default defineConfig({
  plugins: [react(), svgr()],
  optimizeDeps: {
    exclude: ["opendcs-api"], // Replace with actual package name
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
        //changeOrigin: true, // Needed for virtual hosted sites
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
  build: {
    commonjsOptions: {
      include: [/opendcs-api/, /node_modules/],
    },
  },
  test: {
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
    ],
  },
});
