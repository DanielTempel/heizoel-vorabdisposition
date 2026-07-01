import { expect, test } from '@playwright/test'
import { openConfirmedTrackingPage } from './helpers/confirmed-tracking-page'
import { prepareTestServices } from './helpers/services'

test.beforeEach(async ({ request }) => {
  await prepareTestServices(request)
})

test('confirmed appointment shows tracking map with vehicle and destination markers', async ({
  page,
  request,
}) => {
  const { trackingInfo, driverLocation } = await openConfirmedTrackingPage(
    page,
    request,
    'PW-TRACKING-MAP',
  )

  expect(trackingInfo.trackingAvailable).toBe(true)
  expect(trackingInfo.targetLocationX).not.toBeNull()
  expect(trackingInfo.targetLocationY).not.toBeNull()
  expect(driverLocation.locationX).toBeTruthy()
  expect(driverLocation.locationY).toBeTruthy()

  await expect(page.getByText('Live Tracking')).toBeVisible()
  await expect(page.getByTestId('tracking-map')).toBeVisible()
  await expect(page.getByTestId('vehicle-marker')).toBeVisible()
  await expect(page.getByTestId('destination-marker')).toBeVisible()
})
