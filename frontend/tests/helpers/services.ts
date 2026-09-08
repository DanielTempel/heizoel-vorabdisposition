import { randomUUID } from 'node:crypto'
import { expect, type APIRequestContext } from '@playwright/test'
import { BACKEND_URL } from './confirmation-request'
import { MAILPIT_URL } from './mailpit'
import { DISPO_MOCK_URL } from './dispo-mock'

const SERVICE_CHECK_TIMEOUT = 60_000

async function expectServiceAvailable(
  request: APIRequestContext,
  url: string,
  name: string,
) {
  await expect
    .poll(
      async () => {
        try {
          const response = await request.get(url, {
            timeout: 5_000,
          })

          return response.ok()
            ? 'ready'
            : `HTTP ${response.status()}`
        } catch (error) {
          return error instanceof Error
            ? error.message
            : 'Connection failed'
        }
      },
      {
        message: `${name} did not become available at ${url}`,
        timeout: SERVICE_CHECK_TIMEOUT,
        intervals: [1_000, 2_000, 5_000],
      },
    )
    .toBe('ready')
}

async function expectBackendAvailable(request: APIRequestContext) {
  const probeToken = `e2e-readiness-${randomUUID()}`

  await expect
    .poll(
      async () => {
        try {
          const response = await request.get(
            `${BACKEND_URL}/api/customer/confirmations/${probeToken}`,
            { timeout: 5_000 },
          )

          const body = await response.json().catch(() => null)

          return {
            status: response.status(),
            code: body?.code ?? null,
          }
        } catch {
          return {
            status: 0,
            code: 'CONNECTION_FAILED',
          }
        }
      },
      {
        message: `Backend did not become available at ${BACKEND_URL}`,
        timeout: SERVICE_CHECK_TIMEOUT,
        intervals: [1_000, 2_000, 5_000],
      },
    )
    .toEqual({
      status: 404,
      code: 'CONFIRMATION_REQUEST_NOT_FOUND',
    })
}

export async function prepareTestServices(
  request: APIRequestContext,
) {
  await expectBackendAvailable(request)

  await expectServiceAvailable(
    request,
    `${MAILPIT_URL}/api/v1/messages`,
    'Mailpit',
  )

  await expectServiceAvailable(
    request,
    `${DISPO_MOCK_URL}/api/dispo/confirmation-status-updates`,
    'DISPO mock',
  )
}
