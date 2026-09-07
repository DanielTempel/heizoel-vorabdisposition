import { randomUUID } from 'node:crypto'
import { expect, test } from '@playwright/test'
import { createConfirmationRequest } from '../helpers/confirmation-request'
import { openConfirmedTrackingPage } from '../helpers/confirmed-tracking-page'
import { waitForConfirmationLink } from '../helpers/mailpit'

test.describe('Delivery tracking', () => {
  test(
    'shows the vehicle and destination markers',
    async ({ page, request }) => {
      const {
        trackingInfo,
        driverLocation,
      } = await openConfirmedTrackingPage(
        page,
        request,
        'PW-TRACKING-MAP',
      )

      expect(
        trackingInfo.trackingAvailable,
      ).toBe(true)

      expect(
        trackingInfo.targetLocationX,
      ).not.toBeNull()

      expect(
        trackingInfo.targetLocationY,
      ).not.toBeNull()

      expect(
        driverLocation.locationX,
      ).toEqual(expect.any(Number))

      expect(
        driverLocation.locationY,
      ).toEqual(expect.any(Number))

      await expect(
        page.getByText('Live Tracking'),
      ).toBeVisible()

      await expect(
        page.getByTestId('tracking-map'),
      ).toBeVisible()

      await expect(
        page.getByTestId('vehicle-marker'),
      ).toBeVisible()

      await expect(
        page.getByTestId('destination-marker'),
      ).toBeVisible()
    },
  )

  test(
    'shows the delivery address as the destination',
    async ({ page, request }) => {
      const {
        trackingInfo,
      } = await openConfirmedTrackingPage(
        page,
        request,
        'PW-TRACKING-ADDRESS',
      )

      expect(
        trackingInfo.targetLocationX,
      ).not.toBeNull()

      expect(
        trackingInfo.targetLocationY,
      ).not.toBeNull()

      await expect(
        page.getByText(
          'Zieladresse: Domstraße 40, 97070 Würzburg',
        ),
      ).toBeVisible()

      await expect(
        page.getByText(
          'Sie können den Lieferstatus auf dieser Seite verfolgen.',
        ),
      ).toBeVisible()

      await expect(
        page.getByTestId('destination-marker'),
      ).toBeVisible()
    },
  )

  test(
    'does not show tracking before the delivery date',
    async ({ page, request }) => {
      const uniqueId = randomUUID()
      const externalOrderId =
        `PW-TRACKING-FUTURE-${uniqueId}`
      const customerEmail =
        `pw-tracking-future-${uniqueId}@example.com`

      await createConfirmationRequest(
        request,
        externalOrderId,
        customerEmail,
        { deliveryDateOffsetDays: 1 },
      )

      const confirmationLink =
        await waitForConfirmationLink(
          request,
          customerEmail,
        )

      await page.goto(confirmationLink)

      const trackingInfoResponsePromise =
        page.waitForResponse(
          (response) =>
            response
              .url()
              .includes('/tracking-info') &&
            response.request().method() === 'GET' &&
            response.ok(),
        )

      await page
        .getByRole('button', {
          name: 'Termin bestätigen',
        })
        .click()

      await expect(
        page.getByText(
          'Der Liefertermin wurde bestätigt.',
        ),
      ).toBeVisible()

      const trackingInfoResponse =
        await trackingInfoResponsePromise
      const trackingInfo =
        (await trackingInfoResponse.json()) as {
          trackingAvailable: boolean
          targetLocationX: number | null
          targetLocationY: number | null
        }

      expect(trackingInfo).toEqual({
        trackingAvailable: false,
        targetLocationX: null,
        targetLocationY: null,
      })

      await expect(
        page.getByText(
          'Tracking-Informationen werden hier erst am Liefertag eingeblendet.',
          { exact: false },
        ),
      ).toBeVisible()

      await expect(
        page.getByTestId('tracking-map'),
      ).toHaveCount(0)

      await expect(
        page.getByTestId('vehicle-marker'),
      ).toHaveCount(0)

      await expect(
        page.getByTestId('destination-marker'),
      ).toHaveCount(0)
    },
  )

  test(
    'updates the driver location and status when refreshed',
    async ({ page, request }) => {
      const {
        driverLocation: initialDriverLocation,
      } = await openConfirmedTrackingPage(
        page,
        request,
        'PW-TRACKING-REFRESH',
      )

      const trackingStatusBadge = page.getByTestId(
        'tracking-status-badge',
      )
      const refreshButton = page.getByRole('button', {
        name: 'Aktualisieren',
      })

      await expect(
        trackingStatusBadge,
      ).toBeVisible()
      await expect(refreshButton).toBeEnabled()

      const initialStatusText = (
        await trackingStatusBadge.textContent()
      )?.trim()

      expect(initialStatusText).toBeTruthy()

      const driverLocationResponsePromise =
        page.waitForResponse(
          (response) =>
            response
              .url()
              .includes('/driver-location') &&
            response.request().method() === 'GET' &&
            response.ok(),
        )

      await refreshButton.click()

      const driverLocationResponse =
        await driverLocationResponsePromise

      const refreshedDriverLocation =
        (await driverLocationResponse.json()) as {
          locationX: number
          locationY: number
        }

      expect([
        refreshedDriverLocation.locationX,
        refreshedDriverLocation.locationY,
      ]).not.toEqual([
        initialDriverLocation.locationX,
        initialDriverLocation.locationY,
      ])

      await expect
        .poll(async () =>
          (
            await trackingStatusBadge.textContent()
          )?.trim(),
        )
        .not.toBe(initialStatusText)

      await expect(
        trackingStatusBadge,
      ).toContainText(/Noch .* km|Angekommen/)

      await expect(
        page.getByTestId('tracking-map'),
      ).toBeVisible()

      await expect(
        page.getByTestId('vehicle-marker'),
      ).toBeVisible()

      await expect(
        page.getByTestId('destination-marker'),
      ).toBeVisible()
    },
  )
})
