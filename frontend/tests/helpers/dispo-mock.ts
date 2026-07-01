import { expect, type APIRequestContext } from '@playwright/test'

export const DISPO_MOCK_URL = 'http://localhost:8090'

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
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
  expectedStatus: 'CONFIRMED',
  timeoutMs = 20_000,
) {
  const deadline = Date.now() + timeoutMs

  while (Date.now() < deadline) {
    const response = await request.get(
      `${DISPO_MOCK_URL}/api/dispo/confirmation-status-updates`,
    )

    expect(response.ok()).toBeTruthy()

    const callbacks = await response.json()

    const callbackExists = callbacks.some(
      (callback: any) =>
        callback.externalOrderId === externalOrderId &&
        callback.confirmationStatus === expectedStatus,
    )

    if (callbackExists) {
      return
    }

    await sleep(500)
  }

  throw new Error(
    `Expected ${expectedStatus} callback for order ${externalOrderId}.`,
  )
}