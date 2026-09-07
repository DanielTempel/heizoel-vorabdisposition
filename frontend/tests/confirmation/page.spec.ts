import { randomUUID } from 'node:crypto'
import { expect, test } from '@playwright/test'
import { createConfirmationRequest } from '../helpers/confirmation-request'
import { waitForConfirmationLink } from '../helpers/mailpit'

test.describe('Confirmation page', () => {
  test('displays appointment details from the email link', async ({
    page,
    request,
  }) => {
    const uniqueId = randomUUID()
    const externalOrderId = `PW-DETAILS-${uniqueId}`
    const customerEmail = `pw-details-${uniqueId}@example.com`

    const deliveryWindow = await createConfirmationRequest(
      request,
      externalOrderId,
      customerEmail,
    )

    const confirmationLink = await waitForConfirmationLink(
      request,
      customerEmail,
    )

    await page.goto(confirmationLink)

    await expect(
      page.getByText('Bestätigen Sie Ihren Liefertermin', {
        exact: false,
      }),
    ).toBeVisible()

    await expect(page.getByText('Max Müller')).toBeVisible()

    await expect(
      page.getByText('Domstraße 40, 97070 Würzburg'),
    ).toBeVisible()

    await expect(
      page.getByText('Heizöl Standard'),
    ).toBeVisible()

    await expect(
      page.getByText('3.000 Liter'),
    ).toBeVisible()

    await expect(
      page.getByText(deliveryWindow.deliveryWindowStart),
    ).toBeVisible()

    await expect(
      page.getByText(deliveryWindow.deliveryWindowEnd),
    ).toBeVisible()

    await expect(
      page.getByText('100 EUR'),
    ).toBeVisible()
  })

  test('shows an error for an unknown confirmation token', async ({
    page,
  }) => {
    await page.goto('/confirmation/invalid-token-123')

    await expect(
      page.getByText('Fehler - Link ungültig'),
    ).toBeVisible()

    await expect(
      page.getByText('Dieser Link ist nicht mehr gültig'),
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
  })
})
