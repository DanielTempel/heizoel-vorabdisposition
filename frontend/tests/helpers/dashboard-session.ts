import { env } from 'node:process'
import {
  expect,
  type APIRequestContext,
  type Page,
} from '@playwright/test'
import { BACKEND_URL } from './confirmation-request'

export async function createDashboardAccessLink(
  request: APIRequestContext,
) {
  const apiKey = env.E2E_API_KEY

  if (!apiKey) {
    throw new Error(
      'E2E_API_KEY environment variable is required',
    )
  }

  const response = await request.post(
    `${BACKEND_URL}/api/dispo/dashboard-access`,
    {
      headers: {
        'X-API-Key': apiKey,
      },
    },
  )
  const responseBody = await response.text()

  await expect(
    response,
    `Failed to create dashboard access link. HTTP ${response.status()}: ${responseBody}`,
  ).toBeOK()

  if (!responseBody.includes('/login?code=')) {
    throw new Error(
      `Backend returned an invalid dashboard access link: ${responseBody}`,
    )
  }

  return responseBody
}

export async function openDashboard(
  page: Page,
  request: APIRequestContext,
) {
  const accessLink =
    await createDashboardAccessLink(request)

  await page.goto(accessLink)

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(
    page.getByRole('heading', {
      name: 'Avisierungsdashboard',
    }),
  ).toBeVisible()
}
