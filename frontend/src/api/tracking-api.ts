import type { DriverLocation, TrackingInfo } from '../types/tracking'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function handleBackendResponse(response: Response) {
  if (response.ok) {
    return response
  }

  throw new Error(`Backend request failed with status ${response.status}`)
}

export async function getDriverLocation(token: string): Promise<DriverLocation> {
  const response = await fetch(
    `${apiBaseUrl}/api/customer/confirmations/${token}/driver-location?t=${Date.now()}`,
    {
      cache: 'no-store',
    },
  )

  return (await handleBackendResponse(response)).json() as Promise<DriverLocation>
}

export async function getTrackingInfo(token: string): Promise<TrackingInfo> {
  const response = await fetch(
    `${apiBaseUrl}/api/customer/confirmations/${token}/tracking-info?t=${Date.now()}`,
    {
      cache: 'no-store',
    },
  )

  return (await handleBackendResponse(response)).json() as Promise<TrackingInfo>
}
