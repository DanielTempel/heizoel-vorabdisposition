import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { formatDate, formatTime } from '../../../lib/format-delivery'
import type {
  CustomerAnswerType,
  CustomerConfirmationPreview,
} from '../../../types/confirmation'

type SuccessStateProps = {
  answerType: CustomerAnswerType | null
  confirmation: CustomerConfirmationPreview
}

export function SuccessState({
  answerType,
  confirmation,
}: SuccessStateProps) {
  const statusText =
    answerType === 'reject'
      ? 'Der Liefertermin wurde abgelehnt.'
      : 'Der Liefertermin wurde bestätigt.'

  return (
    <main className="min-h-screen bg-background px-6 py-16 text-foreground">
      <Card className="mx-auto w-full max-w-6xl rounded-3xl p-4 sm:p-8">
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
    </main>
  )
}
