import { randomUUID } from 'node:crypto'
import {
  expect,
  test,
  type APIRequestContext,
} from '@playwright/test'
import { createConfirmationRequest } from '../helpers/confirmation-request'
import { expectDispoCallback } from '../helpers/dispo-mock'
import { waitForConfirmationLink } from '../helpers/mailpit'

async function createConfirmationScenario(
  request: APIRequestContext,
  scenarioName: string,
) {
  const uniqueId = randomUUID()
  const externalOrderId = `PW-${scenarioName}-${uniqueId}`
  const customerEmail =
    `pw-${scenarioName.toLowerCase()}-${uniqueId}@example.com`

  await createConfirmationRequest(
    request,
    externalOrderId,
    customerEmail,
  )

  const confirmationLink = await waitForConfirmationLink(
    request,
    customerEmail,
  )

  return {
    confirmationLink,
    customerEmail,
    externalOrderId,
  }
}

test.describe('Customer response', () => {
  test(
    'confirms an appointment and sends a CONFIRMED callback to DISPO',
    async ({ page, request }) => {
      const scenario = await createConfirmationScenario(
        request,
        'CONFIRM',
      )

      await page.goto(scenario.confirmationLink)

      await expect(
        page.getByText('Bestätigen Sie Ihren Liefertermin', {
          exact: false,
        }),
      ).toBeVisible()

      await page
        .getByRole('button', {
          name: 'Termin bestätigen',
        })
        .click()

      await expect(
        page.getByText('Der Liefertermin wurde bestätigt.'),
      ).toBeVisible()

      await expectDispoCallback(
        request,
        scenario.externalOrderId,
        'CONFIRMED',
      )
    },
  )

  test(
    'includes the customer comment in the CONFIRMED callback',
    async ({ page, request }) => {
      const scenario = await createConfirmationScenario(
        request,
        'CONFIRM-COMMENT',
      )

      const customerComment =
        'Please call me shortly before delivery.'

      await page.goto(scenario.confirmationLink)

      await page
        .getByLabel(
          'Nachricht an die Disposition (optional)',
        )
        .fill(customerComment)

      await page
        .getByRole('button', {
          name: 'Termin bestätigen',
        })
        .click()

      await expect(
        page.getByText('Der Liefertermin wurde bestätigt.'),
      ).toBeVisible()

      await expectDispoCallback(
        request,
        scenario.externalOrderId,
        'CONFIRMED',
        customerComment,
      )
    },
  )

  test(
    'keeps the confirmed state when the confirmation link is reopened',
    async ({ page, request }) => {
      const scenario = await createConfirmationScenario(
        request,
        'CONFIRMED-STATE',
      )

      await page.goto(scenario.confirmationLink)

      await page
        .getByRole('button', {
          name: 'Termin bestätigen',
        })
        .click()

      await expect(
        page.getByText('Der Liefertermin wurde bestätigt.'),
      ).toBeVisible()

      await expectDispoCallback(
        request,
        scenario.externalOrderId,
        'CONFIRMED',
      )

      await page.goto(scenario.confirmationLink)

      await expect(
        page.getByText('Der Liefertermin wurde bestätigt.'),
      ).toBeVisible()

      await expect(
        page.getByRole('button', {
          name: 'Termin bestätigen',
        }),
      ).toHaveCount(0)

      await expect(
        page.getByRole('button', {
          name: 'Termin ablehnen',
        }),
      ).toHaveCount(0)
    },
  )

  test(
    'rejects an appointment and sends a REJECTED callback to DISPO',
    async ({ page, request }) => {
      const scenario = await createConfirmationScenario(
        request,
        'REJECT',
      )

      await page.goto(scenario.confirmationLink)

      await page
        .getByRole('button', {
          name: 'Termin ablehnen',
        })
        .click()

      await expect(
        page.getByText('Der Liefertermin wurde abgelehnt.'),
      ).toBeVisible()

      await expectDispoCallback(
        request,
        scenario.externalOrderId,
        'REJECTED',
        null,
      )

      await expect(
        page.getByTestId('tracking-map'),
      ).toHaveCount(0)
    },
  )

  test(
    'keeps the rejected state when the confirmation link is reopened',
    async ({ page, request }) => {
      const scenario = await createConfirmationScenario(
        request,
        'REJECTED-STATE',
      )

      await page.goto(scenario.confirmationLink)

      await page
        .getByRole('button', {
          name: 'Termin ablehnen',
        })
        .click()

      await expect(
        page.getByText('Der Liefertermin wurde abgelehnt.'),
      ).toBeVisible()

      await expectDispoCallback(
        request,
        scenario.externalOrderId,
        'REJECTED',
        null,
      )

      await page.goto(scenario.confirmationLink)

      await expect(
        page.getByText('Der Liefertermin wurde abgelehnt.'),
      ).toBeVisible()

      await expect(
        page.getByRole('button', {
          name: 'Termin bestätigen',
        }),
      ).toHaveCount(0)

      await expect(
        page.getByRole('button', {
          name: 'Termin ablehnen',
        }),
      ).toHaveCount(0)

      await expect(
        page.getByTestId('tracking-map'),
      ).toHaveCount(0)
    },
  )
})