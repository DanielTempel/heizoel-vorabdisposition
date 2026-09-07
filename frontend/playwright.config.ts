import path from 'node:path'
import { env } from 'node:process'
import { fileURLToPath } from 'node:url'
import { defineConfig, devices } from '@playwright/test'
import { loadEnv } from 'vite'

const currentDirectory = path.dirname(fileURLToPath(import.meta.url))
const backendEnvironment = loadEnv(
  'development',
  path.resolve(currentDirectory, '../backend'),
  '',
)

env.E2E_API_KEY ??= backendEnvironment.DEV_API_KEY

if (!env.E2E_API_KEY) {
  throw new Error(
    'E2E_API_KEY or DEV_API_KEY must be configured before running E2E tests',
  )
}

export default defineConfig({
  testDir: './tests',

  timeout: 90_000,
  expect: {
    timeout: 10_000,
  },

  fullyParallel: false,
  workers: 1,

  forbidOnly: Boolean(env.CI),
  retries: env.CI ? 2 : 0,

  reporter: env.CI
    ? [['list'], ['html', { open: 'never' }]]
    : [['html', { open: 'never' }]],

  use: {
    baseURL: env.E2E_FRONTEND_URL ?? 'http://localhost:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'setup',
      testMatch: /services\.setup\.ts/,
    },
    {
      name: 'chromium',
      dependencies: ['setup'],
      testIgnore: /services\.setup\.ts/,
      use: {
        ...devices['Desktop Chrome'],
      },
    },
  ],

  outputDir: 'test-results',
})
