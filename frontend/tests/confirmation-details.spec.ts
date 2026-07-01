import { expect, test } from '@playwright/test'
import { createConfirmationRequest } from './helpers/confirmation-request'
import { waitForConfirmationLink } from './helpers/mailpit'
import { prepareTestServices } from './helpers/services'

test.beforeEach(async ({ request }) => {
  await prepareTestServices(request)
})

test('confirmation page displays correct appointment details', async ({
  page,
  request,
}) => {
  const uniqueId = Date.now()
  const externalOrderId = `PW-DETAILS-${uniqueId}`
  const customerEmail = `pw-details-${uniqueId}@example.com`

  await createConfirmationRequest(request, externalOrderId, customerEmail)

  const confirmationLink = await waitForConfirmationLink(
    request,
    customerEmail,
  )

  await page.goto(confirmationLink)

  await expect(
    page.getByText('Bestätigen Sie Ihren Liefertermin', { exact: false }),
  ).toBeVisible()

  await expect(page.getByText('Max Müller')).toBeVisible()
  await expect(page.getByText('Domstraße 40, 97070 Würzburg')).toBeVisible()
  await expect(page.getByText('Heizöl Standard')).toBeVisible()
  await expect(page.getByText('3.000 Liter')).toBeVisible()
  await expect(page.getByText('10:00')).toBeVisible()
  await expect(page.getByText('11:00')).toBeVisible()
  await expect(page.getByText('100 EUR')).toBeVisible()
})