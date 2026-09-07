import { randomUUID } from 'node:crypto'
import { expect, test } from '@playwright/test'
import { createDashboardAccessLink } from '../helpers/dashboard-session'

test.describe('Dashboard authentication', () => {
  test(
    'opens the dashboard with a valid access link',
    async ({ page, request }) => {
      const accessLink =
        await createDashboardAccessLink(request)

      await page.goto(accessLink)

      await expect(page).toHaveURL(/\/dashboard$/)
      await expect(
        page.getByRole('heading', {
          name: 'Avisierungsdashboard',
        }),
      ).toBeVisible()
      await expect(
        page.getByRole('region', {
          name: 'Touren',
        }),
      ).toBeVisible()
    },
  )

  test(
    'shows an error for an invalid dashboard access code',
    async ({ page }) => {
      const invalidCode = `invalid-${randomUUID()}`

      await page.goto(`/login?code=${invalidCode}`)

      await expect(page).toHaveURL(/\/login\?code=/)
      await expect(
        page.getByText(
          'Dashboard-Zugang nicht möglich',
          { exact: true },
        ),
      ).toBeVisible()
      await expect(
        page.getByText(
          'Der Zugangslink fehlt, ist ungültig oder bereits abgelaufen.',
        ),
      ).toBeVisible()
      await expect(
        page.getByRole('heading', {
          name: 'Avisierungsdashboard',
        }),
      ).toHaveCount(0)
    },
  )
})
