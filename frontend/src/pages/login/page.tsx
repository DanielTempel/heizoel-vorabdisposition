import { useEffect, useRef, useState } from 'react'
import { LoaderCircle, ShieldAlert, ShieldCheck } from 'lucide-react'
import { exchangeDashboardAccessCode } from '@/api/dashboard-auth-api'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

type LoginStatus = 'loading' | 'error'

export function LoginPage() {
  const accessCode = new URLSearchParams(window.location.search).get('code')
  const hasStartedExchange = useRef(false)
  const [status, setStatus] = useState<LoginStatus>(
    accessCode ? 'loading' : 'error',
  )

  useEffect(() => {
    if (!accessCode || hasStartedExchange.current) {
      return
    }

    hasStartedExchange.current = true

    exchangeDashboardAccessCode(accessCode)
      .then(() => window.location.replace('/dashboard'))
      .catch(() => setStatus('error'))
  }, [accessCode])

  const isLoading = status === 'loading'

  return (
    <main className="grid min-h-screen place-items-center bg-muted/20 px-4 py-8 text-foreground">
      <Card className="w-full max-w-md py-6">
        <CardHeader className="text-center">
          {isLoading ? (
            <ShieldCheck className="mx-auto mb-2 size-8 text-primary" />
          ) : (
            <ShieldAlert className="mx-auto mb-2 size-8 text-destructive" />
          )}
          <CardTitle className="text-lg">
            {isLoading
              ? 'Dashboard wird geöffnet'
              : 'Dashboard-Zugang nicht möglich'}
          </CardTitle>
          <CardDescription>
            {isLoading
              ? 'Der sichere Zugang wird geprüft.'
              : 'Der Zugangslink fehlt, ist ungültig oder bereits abgelaufen.'}
          </CardDescription>
        </CardHeader>

        {isLoading ? (
          <CardContent className="flex justify-center" aria-live="polite">
            <LoaderCircle className="size-5 animate-spin" aria-hidden="true" />
            <span className="sr-only">Zugang wird geprüft</span>
          </CardContent>
        ) : null}
      </Card>
    </main>
  )
}
