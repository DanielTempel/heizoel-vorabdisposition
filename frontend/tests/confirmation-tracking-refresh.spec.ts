import { expect, test } from '@playwright/test'
import { openConfirmedTrackingPage } from './helpers/confirmed-tracking-page'
import { prepareTestServices } from './helpers/services'

test.beforeEach(async ({ request }) => {
  await prepareTestServices(request)
})

test('tracking map refresh keeps the map visible and updates the driver status', async ({
  page,
  request,
}) => {
  await openConfirmedTrackingPage(page, request, 'PW-TRACKING-REFRESH')

  const initialBadge = page.getByTestId('tracking-status-badge')
  await expect(initialBadge).toBeVisible()

  const driverLocationResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/driver-location') && response.ok(),
  )

  await page.getByRole('button', { name: 'Aktualisieren' }).click()

  await driverLocationResponsePromise

  await expect(page.getByTestId('tracking-map')).toBeVisible()
  await expect(page.getByTestId('vehicle-marker')).toBeVisible()
  await expect(page.getByTestId('destination-marker')).toBeVisible()
  await expect(page.getByTestId('tracking-status-badge')).toContainText(
    /Noch .* km|Angekommen/,
  )
})
