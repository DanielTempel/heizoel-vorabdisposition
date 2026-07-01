import { expect, test } from '@playwright/test'
import { prepareTestServices } from './helpers/services'

test.beforeEach(async ({ request }) => {
  await prepareTestServices(request)
})

test('invalid confirmation link shows error message', async ({ page }) => {
  await page.goto('http://localhost:3000/confirmation/invalid-token-123')

  await expect(page.getByText('Fehler - Link ungültig')).toBeVisible()
    await expect(page.getByText('Dieser Link ist nicht mehr gültig')).toBeVisible()
})