import { ConfirmationStatusBadge } from '@/components/confirmation-status-badge'
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import type {
  CommunicationChannel,
  ConfirmationRequestHistoryItem,
} from '@/types/dashboard'

type RequestCardProps = {
  request: ConfirmationRequestHistoryItem
}

const channelLabels: Record<CommunicationChannel, string> = {
  EMAIL: 'E-Mail',
  SMS: 'SMS',
  WHATSAPP: 'WhatsApp',
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'medium',
  }).format(new Date(`${value}T00:00:00`))
}

function formatDateTime(value: string | null) {
  if (value === null) {
    return '–'
  }

  return new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'Europe/Berlin',
  }).format(new Date(value))
}

function formatTime(value: string) {
  return value.slice(0, 5)
}

function RequestField({
  label,
  value,
}: {
  label: string
  value: string
}) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="mt-0.5 font-medium">{value}</dd>
    </div>
  )
}

export function RequestCard({ request }: RequestCardProps) {
  const response = request.customerResponse

  return (
    <Card>
      <CardHeader>
        <CardTitle>Avisierungsanfrage</CardTitle>
        <CardDescription>
          {channelLabels[request.communicationChannel]}
          {request.active ? ' · Aktiv' : ''}
        </CardDescription>
        <CardAction>
          <ConfirmationStatusBadge status={request.status} />
        </CardAction>
      </CardHeader>

      <CardContent className="grid gap-4">
        <dl className="grid gap-4 sm:grid-cols-2">
          <RequestField
            label="Versendet am"
            value={formatDateTime(request.sentAt)}
          />
          <RequestField
            label="Antwort möglich bis"
            value={
              request.expiresAt === null
                ? `${request.responseDeadlineHours} Std. nach Versand`
                : formatDateTime(request.expiresAt)
            }
          />
        </dl>

        <div className="rounded-md border bg-muted/30 p-3">
          <p className="text-xs font-medium text-muted-foreground">
            Vorgeschlagener Liefertermin
          </p>
          <p className="mt-1 text-sm font-medium">
            {formatDate(request.deliveryDate)} ·{' '}
            {formatTime(request.deliveryWindowStart)}–
            {formatTime(request.deliveryWindowEnd)} Uhr
          </p>
        </div>

        <Separator />

        <div>
          <p className="text-xs text-muted-foreground">Kundenantwort</p>
          {response === null ? (
            <p className="mt-1 text-sm">Keine Kundenantwort vorhanden.</p>
          ) : (
            <div className="mt-1 grid gap-1">
              <p className="text-sm font-medium">
                {response.responseType === 'CONFIRM'
                  ? 'Termin bestätigt'
                  : 'Termin abgelehnt'}{' '}
                · {formatDateTime(response.receivedAt)}
              </p>
              <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                {response.comment || 'Kein Kommentar vorhanden.'}
              </p>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
