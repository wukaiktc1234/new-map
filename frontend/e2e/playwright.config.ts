import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: 1,
  timeout: 120000,
  expect: { timeout: 15000 },
  use: {
    baseURL: "http://localhost:3002",
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    video: "off",
    actionTimeout: 15000,
    navigationTimeout: 30000,
  },
  reporter: [
    ["html", { outputFolder: "../../test-report/playwright-report" }],
    ["list"],
    ["json", { outputFile: "../../test-report/test-results.json" }],
  ],
  projects: [
    {
      name: "auth-setup",
      testMatch: /auth\.setup\.ts/,
      use: {
        ...devices["Desktop Chrome"],
        channel: "chrome",
        launchOptions: {
          executablePath: "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
          args: ["--disable-blink-features=AutomationControlled"],
        },
      },
    },
    {
      name: "authenticated",
      dependencies: ["auth-setup"],
      use: {
        storageState: ".auth/user.json",
        ...devices["Desktop Chrome"],
        channel: "chrome",
        launchOptions: {
          executablePath: "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
          args: ["--disable-blink-features=AutomationControlled"],
        },
      },
      testIgnore: /00-login\.spec\.ts|auth\.setup\.ts/,
    },
    {
      name: "enterprise",
      dependencies: ["auth-setup"],
      use: {
        storageState: ".auth/user.json",
        ...devices["Desktop Chrome"],
        channel: "chrome",
        launchOptions: {
          executablePath: "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
          args: ["--disable-blink-features=AutomationControlled"],
        },
      },
      testDir: "./enterprise/specs",
      testMatch: /\.spec\.ts$/,
    },
    {
      name: "deep-enterprise",
      use: {
        ...devices["Desktop Chrome"],
        channel: "chrome",
        launchOptions: {
          executablePath: "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
          args: ["--disable-blink-features=AutomationControlled"],
        },
      },
      testDir: "./deep-tests",
      testMatch: /enterprise-deep\.spec\.ts/,
    },
    {
      name: "login-tests",
      use: {
        ...devices["Desktop Chrome"],
        channel: "chrome",
        launchOptions: {
          executablePath: "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
          args: ["--disable-blink-features=AutomationControlled"],
        },
      },
      testMatch: /00-login\.spec\.ts/,
    },
  ],
});
