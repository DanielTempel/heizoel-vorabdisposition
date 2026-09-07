import { expect, test } from '@playwright/test'
import { openDashboard } from '../helpers/dashboard-session'

test.describe('Dashboard order detail', () => {
  test.beforeEach(async ({ page, request }) => {
    await openDashboard(page, request)
  })

  test(
    'displays the order and current confirmation request details',
    async ({ page }) => {
      await page.goto(
        '/dashboard/orders/DEMO-TODAY-001',
      )

      await expect(
        page.getByRole('heading', {
          name: 'Auftrag DEMO-TODAY-001',
        }),
      ).toBeVisible()
      await expect(
        page.getByText('Max Müller'),
      ).toBeVisible()
      await expect(
        page.getByText('max.mueller@example.com'),
      ).toBeVisible()
      await expect(
        page.getByText(
          'Musterstraße 10, 97070 Würzburg',
        ),
      ).toBeVisible()
      await expect(
        page.getByText('WÜ-DEMO 100'),
      ).toBeVisible()
      await expect(
        page.getByText('3.000 Liter'),
      ).toBeVisible()

      const currentRequest = page.locator(
        'section[aria-labelledby="current-request-title"]',
      )

      await expect(
        currentRequest.locator(
          '[data-status="CONFIRMED"]',
        ),
      ).toBeVisible()
      await expect(currentRequest).toContainText('E-Mail')
      await expect(currentRequest).toContainText(
        'Termin bestätigt',
      )
    },
  )

  test(
    'displays the confirmation request history',
    async ({ page }) => {
      await page.goto(
        '/dashboard/orders/DEMO-TODAY-001',
      )

      const requestHistory = page.locator(
        'section[aria-labelledby="request-history-title"]',
      )

      await expect(requestHistory).toBeVisible()
      await expect(
        requestHistory.locator(
          '[data-status="REJECTED"]',
        ),
      ).toBeVisible()
      await expect(
        requestHistory.locator(
          '[data-status="NO_RESPONSE"]',
        ),
      ).toBeVisible()
      await expect(requestHistory).toContainText(
        'Historische Ablehnung vor der neuen Anfrage.',
      )
    },
  )

  test(
    'displays the customer response and comment',
    async ({ page }) => {
      await page.goto(
        '/dashboard/orders/DEMO-TODAY-002',
      )

      const currentRequest = page.locator(
        'section[aria-labelledby="current-request-title"]',
      )

      await expect(currentRequest).toContainText(
        'Termin abgelehnt',
      )
      await expect(currentRequest).toContainText(
        'Bitte erst ab 15 Uhr.',
      )
    },
  )

  test(
    'does not allow resend for an ineligible confirmation status',
    async ({ page }) => {
      await page.goto(
        '/dashboard/orders/DEMO-TODAY-001',
      )

      await expect(
        page.locator(
          'section[aria-labelledby="current-request-title"]',
        ),
      ).toContainText('Termin bestätigt')

      await expect(
        page.getByRole('button', {
          name: 'Erneut senden',
        }),
      ).toBeDisabled()
    },
  )

  test(
    'allows selecting a communication channel for an eligible resend',
    async ({ page }) => {
      await page.goto(
        '/dashboard/orders/DEMO-TOMORROW-004',
      )

      await expect(
        page.locator(
          'section[aria-labelledby="current-request-title"]',
        ),
      ).toContainText('Keine Rückmeldung')

      const emailButton = page.getByRole('button', {
        name: 'E-Mail',
        exact: true,
      })
      const smsButton = page.getByRole('button', {
        name: 'SMS',
        exact: true,
      })
      const whatsappButton = page.getByRole('button', {
        name: 'WhatsApp',
        exact: true,
      })
      const resendButton = page.getByRole('button', {
        name: 'Erneut senden',
      })

      await expect(emailButton).toHaveAttribute(
        'aria-pressed',
        'true',
      )
      await expect(resendButton).toBeEnabled()

      await smsButton.click()

      await expect(smsButton).toHaveAttribute(
        'aria-pressed',
        'true',
      )
      await expect(emailButton).toHaveAttribute(
        'aria-pressed',
        'false',
      )
      await expect(resendButton).toBeEnabled()

      await whatsappButton.click()

      await expect(whatsappButton).toHaveAttribute(
        'aria-pressed',
        'true',
      )
      await expect(smsButton).toHaveAttribute(
        'aria-pressed',
        'false',
      )
      await expect(resendButton).toBeEnabled()
    },
  )
})
