import { useEffect, useState } from 'react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import {
  confirmDelivery,
  getConfirmationPreview,
  rejectDelivery,
} from '../../api/confirmation-api'
import { formatDate, formatTime } from '../../lib/format-delivery'
import type {
  CustomerAnswerType,
  CustomerConfirmationPreview,
} from '../../types/confirmation'
import { ErrorState } from './components/error-state'
import { LoadingState } from './components/loading-state'
import { SuccessState } from './components/success-state'

type PageStatus = 'loading' | 'ready' | 'submitting' | 'success' | 'error'

function getTokenFromPath() {
  const pathParts = window.location.pathname.split('/').filter(Boolean)
  const confirmationIndex = pathParts.indexOf('confirmation')

  if (confirmationIndex === -1) {
    return 'mock-token'
  }

  return pathParts[confirmationIndex + 1] ?? 'mock-token'
}

export function ConfirmationPage() {
  const [status, setStatus] = useState<PageStatus>('loading')
  const [confirmation, setConfirmation] =
    useState<CustomerConfirmationPreview | null>(null)
  const [answerType, setAnswerType] = useState<CustomerAnswerType | null>(null)

  const token = getTokenFromPath()

  useEffect(() => {
    async function loadConfirmation() {
      try {
        const preview = await getConfirmationPreview(token)

        setConfirmation(preview)
        setStatus('ready')
      } catch {
        setStatus('error')
      }
    }

    void loadConfirmation()
  }, [token])

  async function submitAnswer(type: CustomerAnswerType) {
    setStatus('submitting')
    setAnswerType(type)

    try {
      if (type === 'confirm') {
        await confirmDelivery(token, {})
      } else {
        await rejectDelivery(token, {})
      }

      setStatus('success')
    } catch {
      setStatus('error')
    }
  }

  if (status === 'loading') {
    return <LoadingState />
  }

  if (status === 'error' || confirmation === null) {
    return <ErrorState />
  }

  if (status === 'success') {
    return (
      <SuccessState answerType={answerType} confirmation={confirmation} />
    )
  }

  const isSubmitting = status === 'submitting'

  return (
    <main className="min-h-screen bg-background px-6 py-6 text-foreground">
      <Card className="mx-auto w-full max-w-6xl gap-7 rounded-3xl p-4 shadow-lg sm:p-8">
        <CardHeader className="px-0">
          <CardTitle className="text-2xl font-semibold">
            Bestätigen Sie Ihren Liefertermin
          </CardTitle>
          <CardDescription className="text-sm">
            Bitte prüfen Sie die geplanten Lieferdaten. Wenn der Termin passt,
            bestätigen Sie die Lieferung. Falls der Termin nicht passt, können
            Sie ihn ablehnen und eine kurze Nachricht hinterlassen.
          </CardDescription>
        </CardHeader>

        <CardContent className="grid gap-7 px-0">
          <section className="rounded-2xl border bg-muted p-6">
            <p className="text-xs font-semibold uppercase text-muted-foreground">
              Lieferdatum
            </p>
            <h2 className="mt-2 text-3xl font-bold">
              {formatDate(confirmation.deliveryDate)}
            </h2>
            <p className="mt-3 font-semibold">
              {formatTime(confirmation.deliveryWindowStart)} -{' '}
              {formatTime(confirmation.deliveryWindowEnd)} Uhr
            </p>
          </section>

          <section>
            <p className="text-xs font-semibold uppercase text-muted-foreground">
              Lieferdetails
            </p>
            <div className="mt-4 grid gap-3">
              <p>
                <strong>Auftragsnummer:</strong> {confirmation.externalOrderId}
              </p>
              <Separator />
              <p>
                <strong>Kunde:</strong> {confirmation.customerName}
              </p>
              <Separator />
              <p>
                <strong>Lieferadresse:</strong>{' '}
                {confirmation.deliveryAddress}
              </p>
              <Separator />
              <p>
                <strong>Produkt:</strong> {confirmation.product}
              </p>
              <Separator />
              <p>
                <strong>Menge:</strong>{' '}
                {confirmation.quantityLiters.toLocaleString('de-DE')} Liter
              </p>
            </div>
          </section>

          <Alert className="border-amber-300 bg-amber-50 p-4">
            <AlertDescription className="text-amber-950">
              <strong>Bitte beachten Sie:</strong> Diese Anfrage kann nur einmal beantwortet werden.
            </AlertDescription>
          </Alert>

          <div className="grid gap-4 sm:grid-cols-2">
            <Button
              className="h-14 rounded-2xl text-base"
              disabled={isSubmitting}
              type="button"
              onClick={() => void submitAnswer('confirm')}
            >
              Termin bestätigen
            </Button>
            <Button
              className="h-14 rounded-2xl text-base"
              variant="destructive"
              disabled={isSubmitting}
              type="button"
              onClick={() => void submitAnswer('reject')}
            >
              Termin ablehnen
            </Button>
          </div>
        </CardContent>
      </Card>
    </main>
  )
}
