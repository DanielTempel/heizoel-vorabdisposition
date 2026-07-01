import { expect, type APIRequestContext, type Page } from '@playwright/test'
import { createConfirmationRequest } from './confirmation-request'
import { waitForConfirmationLink } from './mailpit'

type TrackingInfoResponse = {
  trackingAvailable: boolean
  targetLocationX: number | null
  targetLocationY: number | null
}

type DriverLocationResponse = {
  locationX: number
  locationY: number
}

export async function openConfirmedTrackingPage(
  page: Page,
  request: APIRequestContext,
  scenarioName: string,
) {
  const uniqueId = Date.now()
  const externalOrderId = `${scenarioName}-${uniqueId}`
  const customerEmail = `${scenarioName.toLowerCase()}-${uniqueId}@example.com`

  await createConfirmationRequest(request, externalOrderId, customerEmail)

  const confirmationLink = await waitForConfirmationLink(request, customerEmail)

  await page.goto(confirmationLink)

  const trackingInfoResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/tracking-info') && response.ok(),
  )
  const driverLocationResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/driver-location') && response.ok(),
  )

  await page.getByRole('button', { name: 'Termin bestätigen' }).click()

  await expect(
    page.getByText('Der Liefertermin wurde bestätigt.', { exact: false }),
  ).toBeVisible()

  const [trackingInfoResponse, driverLocationResponse] = await Promise.all([
    trackingInfoResponsePromise,
    driverLocationResponsePromise,
  ])

  const trackingInfo =
    (await trackingInfoResponse.json()) as TrackingInfoResponse
  const driverLocation =
    (await driverLocationResponse.json()) as DriverLocationResponse

  return {
    confirmationLink,
    customerEmail,
    driverLocation,
    externalOrderId,
    trackingInfo,
  }
}
