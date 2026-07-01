import { expect, test } from '@playwright/test'
import { openConfirmedTrackingPage } from './helpers/confirmed-tracking-page'
import { prepareTestServices } from './helpers/services'

test.beforeEach(async ({ request }) => {
  await prepareTestServices(request)
})

test('tracking map shows the delivery address that is marked as the destination', async ({
  page,
  request,
}) => {
  const { trackingInfo } = await openConfirmedTrackingPage(
    page,
    request,
    'PW-TRACKING-ADDRESS',
  )

  expect(trackingInfo.targetLocationX).not.toBeNull()
  expect(trackingInfo.targetLocationY).not.toBeNull()

  await expect(
    page.getByText('Zieladresse: Domstraße 40, 97070 Würzburg'),
  ).toBeVisible()
  await expect(
    page.getByText('Sie können den Lieferstatus auf dieser Seite verfolgen.'),
  ).toBeVisible()
  await expect(page.getByTestId('destination-marker')).toBeVisible()
})
