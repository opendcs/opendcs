import type { StorybookConfig } from "@storybook/react-vite";
import { mergeConfig } from "vite";

const config: StorybookConfig = {
  stories: ["../src/**/*.mdx", "../src/**/*.stories.@(js|jsx|mjs|ts|tsx)"],
  staticDirs: ["../public"],
  addons: [
    "@chromatic-com/storybook",
    "@storybook/addon-vitest",
    "@storybook/addon-a11y",
    "@storybook/addon-docs",
  ],
  framework: "@storybook/react-vite",
  // Pre-bundle react-query / devtools so vitest doesn't trigger a mid-test
  // re-optimize ("Vite unexpectedly reloaded a test") that fails every
  // in-flight story file with "Failed to fetch dynamically imported module".
  viteFinal: async (cfg) =>
    mergeConfig(cfg, {
      optimizeDeps: {
        include: ["@tanstack/react-query", "@tanstack/react-query-devtools"],
      },
    }),
};
export default config;
