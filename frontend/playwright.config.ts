import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  retries: 0,
  use: {
    baseURL: process.env.FIA_BASE_URL || 'http://localhost:8080',
    trace: 'retain-on-failure',
    launchOptions: process.env.FIA_BROWSER_PATH ? { executablePath: process.env.FIA_BROWSER_PATH } : {},
  },
});
