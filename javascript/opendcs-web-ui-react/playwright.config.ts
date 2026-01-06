import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  // ... other configurations
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
    {
      name: "firefox",
      use: { ...devices["Desktop Firefox"] },
    },
    /*
    // Comment out the webkit project to ignore it by default
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    */
  ],
});
