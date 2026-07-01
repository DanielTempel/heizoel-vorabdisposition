import { expect, test } from '@playwright/test'
import { createConfirmationRequest } from './helpers/confirmation-request'
import { expectDispoCallback } from './helpers/dispo-mock'
import { waitForConfirmationLink } from './helpers/mailpit'
import { prepareTestServices } from './helpers/services'

test.beforeEach(async ({ request }) => {
  await prepareTestServices(request)
})

test('customer cannot confirm the same appointment twice', async ({
  page,
  request,
}) => {
  const uniqueId = Date.now()
  const externalOrderId = `PW-DOUBLE-${uniqueId}`
  const customerEmail = `pw-double-${uniqueId}@example.com`

  await createConfirmationRequest(request, externalOrderId, customerEmail)

  const confirmationLink = await waitForConfirmationLink(
    request,
    customerEmail,
  )

  await page.goto(confirmationLink)

  await page.getByRole('button', { name: 'Termin bestätigen' }).click()

  await expect(
    page.getByText('Der Liefertermin wurde bestätigt.'),
  ).toBeVisible()

  await expectDispoCallback(request, externalOrderId, 'CONFIRMED')

  await page.goto(confirmationLink)

  await expect(
    page.getByText('Der Liefertermin wurde bestätigt.', {
      exact: false,
    }),
  ).toBeVisible()
})