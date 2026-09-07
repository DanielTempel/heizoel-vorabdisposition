import { randomUUID } from 'node:crypto'
import {
  expect,
  test,
  type APIRequestContext,
} from '@playwright/test'
import { createConfirmationRequest } from '../helpers/confirmation-request'
import {
  MAILPIT_URL,
  waitForConfirmationLink,
} from '../helpers/mailpit'

type MailpitRecipient = {
  Address: string
}

type MailpitMessageSummary = {
  ID: string
  To?: MailpitRecipient[]
}

type MailpitMessageListResponse =
  | MailpitMessageSummary[]
  | {
      messages?: MailpitMessageSummary[]
    }

type MailpitMessage = {
  Subject?: string
  Text?: string
  HTML?: string
}

async function getLatestMailContentForCustomer(
  request: APIRequestContext,
  customerEmail: string,
) {
  const listResponse = await request.get(
    `${MAILPIT_URL}/api/v1/messages`,
  )

  await expect(
    listResponse,
    'Failed to retrieve messages from Mailpit',
  ).toBeOK()

  const payload =
    (await listResponse.json()) as MailpitMessageListResponse

  const messages = Array.isArray(payload)
    ? payload
    : payload.messages ?? []

  const targetMessage = messages.find((message) =>
    message.To?.some(
      (recipient) => recipient.Address === customerEmail,
    ),
  )

  if (!targetMessage) {
    throw new Error(
      `No Mailpit message was found for ${customerEmail}`,
    )
  }

  const messageResponse = await request.get(
    `${MAILPIT_URL}/api/v1/message/${targetMessage.ID}`,
  )

  await expect(
    messageResponse,
    `Failed to retrieve Mailpit message ${targetMessage.ID}`,
  ).toBeOK()

  const message =
    (await messageResponse.json()) as MailpitMessage

  return [
    message.Subject ?? '',
    message.Text ?? '',
    message.HTML ?? '',
  ].join(' ')
}

test.describe('Confirmation email', () => {
  test(
    'contains the appointment details and confirmation link',
    async ({ request }) => {
      const uniqueId = randomUUID()
      const externalOrderId = `PW-MAIL-${uniqueId}`
      const customerEmail =
        `pw-mail-${uniqueId}@example.com`

      const deliveryWindow =
        await createConfirmationRequest(
          request,
          externalOrderId,
          customerEmail,
        )

      const confirmationLink =
        await waitForConfirmationLink(
          request,
          customerEmail,
        )

      const mailContent =
        await getLatestMailContentForCustomer(
          request,
          customerEmail,
        )

      expect(mailContent).toContain('Max Müller')

      expect(mailContent).toContain(
        'Domstraße 40, 97070 Würzburg',
      )

      expect(mailContent).toContain(
        'Heizöl Standard',
      )

      expect(mailContent).toContain('3000')

      expect(mailContent).toContain(
        deliveryWindow.deliveryWindowStart,
      )

      expect(mailContent).toContain(
        deliveryWindow.deliveryWindowEnd,
      )

      expect(mailContent).toContain('100 EUR')

      expect(mailContent).toContain(
        confirmationLink,
      )
    },
  )
})