import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { ConfirmationCaseDetail } from '@/types/confirmation-cases'
import { channelLabel, DetailGridItem } from './shared'
import { formatDate, formatDateTime, formatTime } from '../utils'

export function RequestContextCard({
  confirmationCase,
}: {
  confirmationCase: ConfirmationCaseDetail
}) {
  const { previousRequest } = confirmationCase

  return (
    <Card className="rounded-3xl border border-slate-200 bg-white shadow-sm">
      <CardHeader className="border-b border-slate-100 px-6 py-5">
        <div className="flex items-center justify-between gap-4">
          <CardTitle>Anfrage und Kundenantwort</CardTitle>
          <span className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-600">
            {previousRequest.active ? 'Aktiv' : 'Inaktiv'}
          </span>
        </div>
      </CardHeader>
      <CardContent className="grid gap-4 px-6 py-6">
        <div className="grid gap-4 sm:grid-cols-3">
          <DetailGridItem
            label="Kanal"
            value={channelLabel(previousRequest.channel)}
          />
          <DetailGridItem
            label="Lieferdatum"
            value={formatDate(previousRequest.deliveryDate)}
          />
          <DetailGridItem
            label="Zeitfenster"
            value={`${formatTime(previousRequest.deliveryWindowStart)} - ${formatTime(previousRequest.deliveryWindowEnd)} Uhr`}
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <DetailGridItem
            label="Antwortfrist"
            value={`${previousRequest.responseDeadlineHours} Stunden`}
          />
          <DetailGridItem
            label="Gesendet am"
            value={formatDateTime(previousRequest.sentAt)}
          />
          <DetailGridItem
            label="Gültig bis"
            value={formatDateTime(previousRequest.validUntil)}
          />
        </div>
        {confirmationCase.customerResponse ? (
          <>
            <div className="border-t border-slate-100 pt-5">
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                Kundenantwort
              </p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <DetailGridItem
                label="Antwort"
                value={confirmationCase.customerResponse.answerLabel}
                valueClassName="text-red-700"
              />
              <DetailGridItem
                label="Antwort erhalten am"
                value={formatDateTime(confirmationCase.customerResponse.receivedAt)}
              />
            </div>
            {confirmationCase.customerResponse.customerComment ? (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  Kommentar des Kunden
                </p>
                <p className="mt-2 text-sm italic text-foreground">
                  "{confirmationCase.customerResponse.customerComment}"
                </p>
              </div>
            ) : null}
          </>
        ) : (
          <Alert className="border-amber-300 bg-amber-50">
            <AlertDescription className="text-amber-950">
              Zu dieser Anfrage ist bisher keine Kundenantwort eingegangen.
            </AlertDescription>
          </Alert>
        )}
      </CardContent>
    </Card>
  )
}
