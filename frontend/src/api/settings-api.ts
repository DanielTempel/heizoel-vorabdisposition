import { ApiError } from './dashboard-api'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export type SmtpSecurityMode = 'STARTTLS' | 'IMPLICIT_TLS' | 'NONE'

export type EmailSettings = {
  configured: boolean
  smtpHost: string | null
  smtpPort: number | null
  securityMode: SmtpSecurityMode | null
  authenticationEnabled: boolean
  username: string | null
  passwordConfigured: boolean
  fromAddress: string | null
  fromName: string | null
  updatedAt: string | null
}

type BackendErrorResponse = {
  code?: string
  message?: string
}

export async function getEmailSettings(
  signal?: AbortSignal,
): Promise<EmailSettings> {
  const response = await fetch(`${apiBaseUrl}/api/dashboard/settings/email`, {
    cache: 'no-store',
    credentials: 'include',
    signal,
  })

  if (!response.ok) {
    const errorResponse = (await response
      .json()
      .catch(() => null)) as BackendErrorResponse | null

    throw new ApiError(
      response.status,
      errorResponse?.code ?? null,
      errorResponse?.message ??
        `Settings request failed with status ${response.status}.`,
    )
  }

  return response.json() as Promise<EmailSettings>
}
