import { expect, type APIRequestContext } from '@playwright/test'

export const DISPO_MOCK_URL = 'http://localhost:8090'

type ConfirmationStatus = 'CONFIRMED' | 'REJECTED'

type DispoCallback = {
  externalOrderId: string
  confirmationStatus: ConfirmationStatus
  customerComment: string | null
}

export async function clearDispoCallbacks(request: APIRequestContext) {
  const response = await request.delete(
    `${DISPO_MOCK_URL}/api/dispo/confirmation-status-updates`,
  )

  expect(response.ok()).toBeTruthy()
}

export async function expectDispoCallback(
  request: APIRequestContext,
  externalOrderId: string,
  expectedStatus: ConfirmationStatus,
  expectedComment?: string | null,
  timeoutMs = 20_000,
) {
  await expect
    .poll(
      async () => {
        const response = await request.get(
          `${DISPO_MOCK_URL}/api/dispo/confirmation-status-updates`,
        )

        await expect(response).toBeOK()

        const callbacks = (await response.json()) as DispoCallback[]

        return (
          callbacks.find(
            (callback) =>
              callback.externalOrderId === externalOrderId &&
              callback.confirmationStatus === expectedStatus,
          ) ?? null
        )
      },
      {
        message: `Expected ${expectedStatus} callback for order ${externalOrderId}`,
        timeout: timeoutMs,
        intervals: [500],
      },
    )
    .toMatchObject({
      externalOrderId,
      confirmationStatus: expectedStatus,
      ...(expectedComment === undefined
        ? {}
        : { customerComment: expectedComment }),
    })
}
