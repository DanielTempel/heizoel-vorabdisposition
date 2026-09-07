import { test as setup } from '@playwright/test'
import { prepareTestServices } from '../helpers/services'

setup('verify required services', async ({ request }) => {
  await prepareTestServices(request)
})
