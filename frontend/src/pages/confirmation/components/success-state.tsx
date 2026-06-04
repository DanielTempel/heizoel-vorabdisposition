import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { formatDate, formatTime } from '../../../lib/format-delivery'
import type {
  CustomerAnswerType,
  CustomerConfirmationPreview,
} from '../../../types/confirmation'
import { TrackingMapCard } from './tracking-map-card'

type SuccessStateProps = {
  answerType: CustomerAnswerType | null
  confirmation: CustomerConfirmationPreview
}

export function SuccessState({
  answerType,
  confirmation,
}: SuccessStateProps) {
  const resolvedAnswerType =
    answerType ??
    (confirmation.confirmationStatus === 'REJECTED' ? 'reject' : 'confirm')
  const isRejected = resolvedAnswerType === 'reject'
  const statusText =
    isRejected
      ? 'Der Liefertermin wurde abgelehnt.'
      : 'Der Liefertermin wurde bestätigt.'

  return (
    <main className="min-h-screen bg-background px-6 py-16 text-foreground">
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-6">
        <Card className="rounded-3xl p-4 sm:p-8">
          <CardHeader className="flex flex-col items-center gap-3 px-0 text-center">
            <p className="text-xs font-semibold uppercase text-muted-foreground">
              Rückmeldung erhalten
            </p>
            <CardTitle className="max-w-2xl text-2xl font-semibold">
              {statusText}
            </CardTitle>
          </CardHeader>
          <CardContent className="mx-auto grid w-full max-w-5xl gap-4 px-0 pt-8">
            <p>
              <strong>Lieferdatum:</strong>{' '}
              {formatDate(confirmation.deliveryDate)} -{' '}
              {formatTime(confirmation.deliveryWindowStart)} -{' '}
              {formatTime(confirmation.deliveryWindowEnd)} Uhr
            </p>
            <Separator />
            <p>
              <strong>Lieferadresse:</strong> {confirmation.deliveryAddress}
            </p>
            <Separator />
            <p>
              <strong>Produkt / Menge:</strong> {confirmation.product} -{' '}
              {confirmation.quantityLiters.toLocaleString('de-DE')} Liter
            </p>

            <p className="mt-8 text-center font-semibold">
              Sie können dieses Fenster nun schließen.
            </p>
          </CardContent>
        </Card>

        {!isRejected ? <TrackingMapCard confirmation={confirmation} /> : null}
      </div>
    </main>
  )
}
