import { expect, test, type APIRequestContext } from '@playwright/test'
import {
  BACKEND_URL,
  createConfirmationRequest,
} from './helpers/confirmation-request'
import { MAILPIT_URL, waitForConfirmationLink } from './helpers/mailpit'
import {
  DISPO_MOCK_URL,
  clearDispoCallbacks,
  expectDispoCallback,
} from './helpers/dispo-mock'
import { prepareTestServices } from './helpers/services'

test.beforeEach(async ({ request }) => {
  await prepareTestServices(request)
})

test('customer confirms delivery appointment via email link', async ({
  page,
  request,
}) => {
  const uniqueId = Date.now()
  const externalOrderId = `PW-CONFIRM-${uniqueId}`
  const customerEmail = `pw-confirm-${uniqueId}@example.com`

  await createConfirmationRequest(request, externalOrderId, customerEmail)

  const confirmationLink = await waitForConfirmationLink(
    request,
    customerEmail,
  )

  await page.goto(confirmationLink)

  await expect(
    page.getByText('Bestätigen Sie Ihren Liefertermin', { exact: false }),
  ).toBeVisible()

  await page.getByRole('button', { name: 'Termin bestätigen' }).click()

  await expect(
    page.getByText('Der Liefertermin wurde bestätigt.',
    ),
  ).toBeVisible()

  await expectDispoCallback(request, externalOrderId, 'CONFIRMED')
})