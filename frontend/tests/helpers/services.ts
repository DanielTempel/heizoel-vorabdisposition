import { expect, type APIRequestContext } from '@playwright/test'
import { BACKEND_URL } from './confirmation-request'
import { MAILPIT_URL } from './mailpit'
import { DISPO_MOCK_URL, clearDispoCallbacks } from './dispo-mock'

async function expectServiceAvailable(
  request: APIRequestContext,
  url: string,
  name: string,
) {
  const response = await request.get(url)

  expect(
    response.ok(),
    `${name} is not reachable at ${url}`,
  ).toBeTruthy()
}

export async function prepareTestServices(request: APIRequestContext) {
  await expectServiceAvailable(
    request,
    `${BACKEND_URL}/swagger-ui.html`,
    'Backend',
  )

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

  await clearDispoCallbacks(request)
}