import {
  expect,
  test,
  type APIRequestContext,
  type Page,
} from '@playwright/test'
import { openDashboard } from '../helpers/dashboard-session'
import { MAILPIT_URL } from '../helpers/mailpit'

type MailpitMessageSummary = {
  ID: string
  Subject?: string
}

type MailpitMessageListResponse =
  | MailpitMessageSummary[]
  | {
      messages?: MailpitMessageSummary[]
    }

async function getMailpitMessages(
  request: APIRequestContext,
) {
  const response = await request.get(
    `${MAILPIT_URL}/api/v1/messages`,
  )

  await expect(
    response,
    'Failed to retrieve messages from Mailpit',
  ).toBeOK()

  const payload =
    (await response.json()) as MailpitMessageListResponse

  return Array.isArray(payload)
    ? payload
    : payload.messages ?? []
}

async function openEmailSettings(page: Page) {
  await page
    .getByRole('link', {
      name: 'Einstellungen öffnen',
    })
    .click()

  await expect(page).toHaveURL(/\/dashboard\/settings$/)
  await expect(
    page.getByRole('heading', {
      name: 'Einstellungen',
    }),
  ).toBeVisible()
  await expect(
    page.getByLabel('SMTP-Server'),
  ).toBeVisible()
}

test.describe('Dashboard email settings', () => {
  test.beforeEach(async ({ page, request }) => {
    await openDashboard(page, request)
    await openEmailSettings(page)
  })

  test(
    'loads the configured email settings',
    async ({ page }) => {
      await expect(
        page.getByLabel('SMTP-Server'),
      ).toHaveValue('mailpit')
      await expect(
        page.getByLabel('Port'),
      ).toHaveValue('1025')
      await expect(
        page.getByLabel('Verschlüsselung'),
      ).toHaveValue('NONE')
      await expect(
        page.getByLabel(
          'SMTP-Authentifizierung verwenden',
        ),
      ).not.toBeChecked()
      await expect(
        page.getByLabel('Absenderadresse'),
      ).toHaveValue('dispo@heizoel.local')
      await expect(
        page.getByLabel('Absendername'),
      ).toHaveValue('Heizöl Disposition')
    },
  )

  test(
    'successfully tests the saved SMTP connection',
    async ({ page }) => {
      const connectionResponsePromise =
        page.waitForResponse(
          (response) =>
            response.url().endsWith(
              '/api/dashboard/settings/email/test-connection',
            ) &&
            response.request().method() === 'POST' &&
            response.ok(),
        )

      await page
        .getByRole('button', {
          name: 'Verbindung testen',
        })
        .click()

      await connectionResponsePromise
      await expect(
        page.getByText(
          'Die Verbindung zum SMTP-Server war erfolgreich.',
        ),
      ).toBeVisible()
    },
  )

  test(
    'sends an SMTP test email',
    async ({ page, request }) => {
      const existingMessageIds = new Set(
        (await getMailpitMessages(request)).map(
          (message) => message.ID,
        ),
      )
      const messageResponsePromise =
        page.waitForResponse(
          (response) =>
            response.url().endsWith(
              '/api/dashboard/settings/email/test-message',
            ) &&
            response.request().method() === 'POST' &&
            response.ok(),
        )

      await page
        .getByRole('button', {
          name: 'Test-E-Mail senden',
        })
        .click()

      await messageResponsePromise
      await expect(
        page.getByText(
          'Der SMTP-Server hat die Test-E-Mail für dispo@heizoel.local angenommen.',
        ),
      ).toBeVisible()

      await expect
        .poll(
          async () => {
            const messages =
              await getMailpitMessages(request)


            return messages.some(
              (message) =>
                !existingMessageIds.has(message.ID) &&
                message.Subject ===
                  'SMTP-Konfiguration erfolgreich getestet',
            )
          },
          {
            message:
              'SMTP test email did not arrive in Mailpit',
            timeout: 20_000,
            intervals: [500],
          },
        )
        .toBe(true)
    },
  )
})
