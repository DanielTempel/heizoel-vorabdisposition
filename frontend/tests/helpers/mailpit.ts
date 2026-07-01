import { expect, type APIRequestContext } from '@playwright/test'

export const MAILPIT_URL = 'http://localhost:8025'

const CONFIRMATION_LINK_PATTERN =
  /http:\/\/localhost:3000\/confirmation\/[A-Za-z0-9_-]+/

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function waitForConfirmationLink(
  request: APIRequestContext,
  customerEmail: string,
  timeoutMs = 20_000,
) {
  const deadline = Date.now() + timeoutMs

  while (Date.now() < deadline) {
    const listResponse = await request.get(`${MAILPIT_URL}/api/v1/messages`)
    expect(listResponse.ok()).toBeTruthy()

    const payload = await listResponse.json()
    const messages = payload.messages ?? payload

    const targetMessage = messages.find((message: any) =>
      message.To?.some((recipient: any) => recipient.Address === customerEmail),
    )

    if (targetMessage) {
      const messageResponse = await request.get(
        `${MAILPIT_URL}/api/v1/message/${targetMessage.ID}`,
      )

      expect(messageResponse.ok()).toBeTruthy()

      const message = await messageResponse.json()
      const mailContent = `${message.Text ?? ''} ${message.HTML ?? ''}`

      const match = mailContent.match(CONFIRMATION_LINK_PATTERN)

      if (match) {
        return match[0]
      }
    }

    await sleep(500)
  }

  throw new Error('Confirmation email link did not arrive in Mailpit.')
}