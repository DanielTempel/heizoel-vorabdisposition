import { ApiError, getCsrfToken } from './dashboard-api'

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

export type UpdateEmailSettingsInput = {
  smtpHost: string
  smtpPort: number
  securityMode: SmtpSecurityMode
  authenticationEnabled: boolean
  username: string | null
  password: string | null
  fromAddress: string
  fromName: string
}

type BackendErrorResponse = {
  code?: string
  message?: string
}

async function createSettingsApiError(response: Response) {
  const errorResponse = (await response
    .json()
    .catch(() => null)) as BackendErrorResponse | null

  return new ApiError(
    response.status,
    errorResponse?.code ?? null,
    errorResponse?.message ??
      `Settings request failed with status ${response.status}.`,
  )
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
    throw await createSettingsApiError(response)
  }

  return response.json() as Promise<EmailSettings>
}

export async function updateEmailSettings(
  input: UpdateEmailSettingsInput,
  signal?: AbortSignal,
): Promise<void> {
  const csrfToken = await getCsrfToken(signal)
  const response = await fetch(`${apiBaseUrl}/api/dashboard/settings/email`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      [csrfToken.headerName]: csrfToken.token,
    },
    body: JSON.stringify(input),
    credentials: 'include',
    signal,
  })

  if (!response.ok) {
    throw await createSettingsApiError(response)
  }
}

async function runEmailSettingsAction(
  action: 'test-connection' | 'test-message',
  signal?: AbortSignal,
): Promise<void> {
  const csrfToken = await getCsrfToken(signal)
  const response = await fetch(
    `${apiBaseUrl}/api/dashboard/settings/email/${action}`,
    {
      method: 'POST',
      headers: {
        [csrfToken.headerName]: csrfToken.token,
      },
      credentials: 'include',
      signal,
    },
  )

  if (!response.ok) {
    throw await createSettingsApiError(response)
  }
}

export function testEmailConnection(signal?: AbortSignal): Promise<void> {
  return runEmailSettingsAction('test-connection', signal)
}

export function sendTestEmail(signal?: AbortSignal): Promise<void> {
  return runEmailSettingsAction('test-message', signal)
}
