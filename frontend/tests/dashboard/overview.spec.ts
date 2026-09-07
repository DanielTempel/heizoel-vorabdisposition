import {
  expect,
  test,
  type Page,
} from '@playwright/test'
import { openDashboard } from '../helpers/dashboard-session'

function getTourToggles(page: Page) {
  return page
    .getByRole('region', { name: 'Touren' })
    .locator('button[aria-expanded]')
}

function getTourToggle(
  page: Page,
  tourNumber: string,
) {
  return getTourToggles(page)
    .filter({ hasText: tourNumber })
}

test.describe('Dashboard overview', () => {
  test.beforeEach(async ({ page, request }) => {
    await openDashboard(page, request)
  })

  test(
    'displays tours with their orders and status counts',
    async ({ page }) => {
      const tourToggle = getTourToggle(page, 'A-17')

      await expect(tourToggle).toBeVisible()
      await expect(tourToggle).toContainText('Bestätigt: 2')
      await expect(tourToggle).toContainText('Abgelehnt: 2')
      await expect(tourToggle).toContainText(
        'Keine Rückmeldung: 1',
      )

      await tourToggle.click()

      const orderRow = page
        .getByRole('row')
        .filter({ hasText: 'DEMO-TODAY-001' })

      await expect(orderRow).toBeVisible()
      await expect(orderRow).toContainText('Max Müller')
      await expect(orderRow).toContainText(
        'Musterstraße 10, 97070 Würzburg',
      )
      await expect(orderRow).toContainText('Bestätigt')
    },
  )

  test(
    'filters orders by customer, order number, or address',
    async ({ page }) => {
      const searchInput = page.getByPlaceholder(
        'Auftrag, Kunde oder Adresse',
      )

      await searchInput.fill('Spezialadresse Suche')
      await searchInput.press('Enter')

      const tourToggle = getTourToggle(page, 'T-ALPHA')

      await expect(tourToggle).toBeVisible()
      await expect(
        getTourToggles(page),
      ).toHaveCount(1)

      await tourToggle.click()

      await expect(
        page.getByRole('row').filter({
          hasText: 'DEMO-ALPHA-002',
        }),
      ).toBeVisible()
      await expect(
        page.getByRole('row').filter({
          hasText: 'DEMO-ALPHA-001',
        }),
      ).toHaveCount(0)
    },
  )

  test(
    'filters tours by confirmation status and tour number',
    async ({ page }) => {
      await page
        .getByRole('button', {
          name: 'Filter',
          exact: true,
        })
        .click()

      await page
        .getByPlaceholder('Tour auswählen')
        .click()
      await page
        .getByRole('option', {
          name: 'A-17',
          exact: true,
        })
        .click()
      await page.keyboard.press('Escape')

      await page
        .getByPlaceholder('Status auswählen')
        .click()
      await page
        .getByRole('option', {
          name: 'Abgelehnt',
          exact: true,
        })
        .click()
      await page.keyboard.press('Escape')

      await page
        .getByRole('button', {
          name: 'Anwenden',
          exact: true,
        })
        .click()

      const tourToggle = getTourToggle(page, 'A-17')

      await expect(tourToggle).toBeVisible()
      await expect(
        getTourToggles(page),
      ).toHaveCount(1)

      await tourToggle.click()

      const orderRows = page.locator('tbody tr')

      await expect(orderRows).toHaveCount(2)
      await expect(orderRows.nth(0)).toContainText(
        'Abgelehnt',
      )
      await expect(orderRows.nth(1)).toContainText(
        'Abgelehnt',
      )
      await expect(
        page.getByRole('row').filter({
          hasText: 'DEMO-TODAY-002',
        }),
      ).toBeVisible()
      await expect(
        page.getByRole('row').filter({
          hasText: 'DEMO-TODAY-005',
        }),
      ).toBeVisible()
    },
  )

  test(
    'navigates between dashboard result pages',
    async ({ page }) => {
      const secondPageLink = page.getByRole('link', {
        name: 'Seite 2',
      })

      await expect(secondPageLink).toBeVisible()

      const secondPageResponsePromise =
        page.waitForResponse((response) => {
          const url = new URL(response.url())

          return (
            url.pathname === '/api/dashboard/tours' &&
            url.searchParams.get('page') === '1' &&
            response.ok()
          )
        })

      await secondPageLink.click()

      const secondPageResponse =
        await secondPageResponsePromise
      const secondPage =
        (await secondPageResponse.json()) as {
          page: number
          items: unknown[]
        }

      expect(secondPage.page).toBe(1)
      expect(secondPage.items.length).toBeGreaterThan(0)
      await expect(secondPageLink).toHaveAttribute(
        'aria-current',
        'page',
      )
      expect(
        await getTourToggles(page).count(),
      ).toBeGreaterThan(0)
    },
  )
})
