import type { DriverLocation, TrackingInfo } from '../types/tracking'

type TrackingApiMode = 'mock' | 'backend'

const apiMode = getApiMode()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const mockTrackingInfo: TrackingInfo = {
  trackingAvailable: true,
  targetLocationX: 9.9372,
  targetLocationY: 49.7935,
}

const mockDriverRoute = [
  { locationX: 9.882, locationY: 49.8166 },
  { locationX: 9.8974, locationY: 49.8108 },
  { locationX: 9.9149, locationY: 49.804 },
  { locationX: 9.9281, locationY: 49.7975 },
]

let mockDriverRouteIndex = 0

function getApiMode(): TrackingApiMode {
  const mode = import.meta.env.VITE_CONFIRMATION_API_MODE

  if (mode === 'mock') {
    return 'mock'
  }

  return 'backend'
}

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

async function handleBackendResponse(response: Response) {
  if (response.ok) {
    return response
  }

  throw new Error(`Backend request failed with status ${response.status}`)
}

export async function getDriverLocation(token: string): Promise<DriverLocation> {
  if (apiMode === 'mock') {
    console.log('Using mock driver location:', { token })

    await delay(300)

    const currentLocation = mockDriverRoute[mockDriverRouteIndex]
    mockDriverRouteIndex = (mockDriverRouteIndex + 1) % mockDriverRoute.length

    return currentLocation
  }

  const response = await fetch(
    `${apiBaseUrl}/api/customer/confirmations/${token}/driver-location?t=${Date.now()}`,
    {
      cache: 'no-store',
    },
  )

  return (await handleBackendResponse(response)).json() as Promise<DriverLocation>
}

export async function getTrackingInfo(token: string): Promise<TrackingInfo> {
  if (apiMode === 'mock') {
    console.log('Using mock tracking info:', { token })

    await delay(300)

    return { ...mockTrackingInfo }
  }

  const response = await fetch(
    `${apiBaseUrl}/api/customer/confirmations/${token}/tracking-info?t=${Date.now()}`,
    {
      cache: 'no-store',
    },
  )

  return (await handleBackendResponse(response)).json() as Promise<TrackingInfo>
}
