import type {
  DashboardFilters,
  OrderDetail,
  ResendConfirmationInput,
  ResendConfirmationResult,
  ToursPage,
} from '../types/dashboard'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

type GetToursInput = DashboardFilters & {
  page: number
}

type BackendErrorResponse = {
  code: string
  message: string
}

export type CsrfTokenResponse = {
  token: string
  headerName: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string | null

  constructor(
    status: number,
    code: string | null,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

async function createApiError(response: Response) {
  const errorResponse = (await response
    .json()
    .catch(() => null)) as Partial<BackendErrorResponse> | null

  return new ApiError(
    response.status,
    errorResponse?.code ?? null,
    errorResponse?.message ??
      `Dashboard request failed with status ${response.status}.`,
  )
}

async function readJsonResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw await createApiError(response)
  }

  return response.json() as Promise<T>
}

export async function getTours(
  input: GetToursInput,
  signal?: AbortSignal,
): Promise<ToursPage> {
  const searchParams = new URLSearchParams({ page: input.page.toString() })

  if (input.search.trim() !== '') {
    searchParams.set('search', input.search.trim())
  }

  input.statuses.forEach((status) => searchParams.append('statuses', status))
  input.tourNumbers.forEach((tourNumber) =>
    searchParams.append('tourNumbers', tourNumber),
  )

  if (input.dateFrom !== '') {
    searchParams.set('dateFrom', input.dateFrom)
  }

  if (input.dateTo !== '') {
    searchParams.set('dateTo', input.dateTo)
  }

  const response = await fetch(
    `${apiBaseUrl}/api/dashboard/tours?${searchParams}`,
    {
      cache: 'no-store',
      credentials: 'include',
      signal,
    },
  )

  return readJsonResponse<ToursPage>(response)
}

export async function getTourNumbers(
  signal?: AbortSignal,
): Promise<string[]> {
  const response = await fetch(`${apiBaseUrl}/api/dashboard/tour-numbers`, {
    cache: 'no-store',
    credentials: 'include',
    signal,
  })

  return readJsonResponse<string[]>(response)
}

export async function getOrderDetail(
  externalOrderId: string,
  signal?: AbortSignal,
): Promise<OrderDetail> {
  const response = await fetch(
    `${apiBaseUrl}/api/dashboard/orders/${encodeURIComponent(externalOrderId)}`,
    {
      cache: 'no-store',
      credentials: 'include',
      signal,
    },
  )

  return readJsonResponse<OrderDetail>(response)
}

export async function getCsrfToken(signal?: AbortSignal) {
  const response = await fetch(`${apiBaseUrl}/api/dashboard/csrf`, {
    cache: 'no-store',
    credentials: 'include',
    signal,
  })

  return readJsonResponse<CsrfTokenResponse>(response)
}

export async function resendConfirmation(
  externalOrderId: string,
  input: ResendConfirmationInput,
  signal?: AbortSignal,
): Promise<ResendConfirmationResult> {
  const csrfToken = await getCsrfToken(signal)
  const response = await fetch(
    `${apiBaseUrl}/api/dashboard/orders/${encodeURIComponent(externalOrderId)}/resend`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        [csrfToken.headerName]: csrfToken.token,
      },
      body: JSON.stringify(input),
      credentials: 'include',
      signal,
    },
  )

  return readJsonResponse<ResendConfirmationResult>(response)
}
