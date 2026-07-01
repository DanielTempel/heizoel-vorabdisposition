import { expect, test, type APIRequestContext } from '@playwright/test'
import { createConfirmationRequest } from './helpers/confirmation-request'
import { MAILPIT_URL, waitForConfirmationLink } from './helpers/mailpit'
import { prepareTestServices } from './helpers/services'

async function getLatestMailContentForCustomer(
  request: APIRequestContext,
  customerEmail: string,
) {
  const listResponse = await request.get(`${MAILPIT_URL}/api/v1/messages`)
  expect(listResponse.ok()).toBeTruthy()

  const payload = await listResponse.json()
  const messages = payload.messages ?? payload

  const targetMessage = messages.find((message: any) =>
    message.To?.some((recipient: any) => recipient.Address === customerEmail),
  )

  expect(targetMessage).toBeTruthy()

  const messageResponse = await request.get(
    `${MAILPIT_URL}/api/v1/message/${targetMessage.ID}`,
  )

  expect(messageResponse.ok()).toBeTruthy()

  const message = await messageResponse.json()

  return `${message.Subject ?? ''} ${message.Text ?? ''} ${message.HTML ?? ''}`
}

test.beforeEach(async ({ request }) => {
  await prepareTestServices(request)
})

test('confirmation email contains correct appointment information', async ({
  request,
}) => {
  const uniqueId = Date.now()
  const externalOrderId = `PW-MAIL-${uniqueId}`
  const customerEmail = `pw-mail-${uniqueId}@example.com`

  await createConfirmationRequest(request, externalOrderId, customerEmail)

  const confirmationLink = await waitForConfirmationLink(
    request,
    customerEmail,
  )

  const mailContent = await getLatestMailContentForCustomer(
    request,
    customerEmail,
  )

  expect(mailContent).toContain('Max Müller')
  expect(mailContent).toContain('Domstraße 40, 97070 Würzburg')
  expect(mailContent).toContain('Heizöl Standard')
  expect(mailContent).toContain('3000')
  expect(mailContent).toContain('10:00')
  expect(mailContent).toContain('11:00')
  expect(mailContent).toContain('100 EUR')
  expect(mailContent).toContain(confirmationLink)
})