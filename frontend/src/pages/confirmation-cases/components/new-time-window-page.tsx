import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, CheckCircle2, Mail, MessageCircle, Phone, RotateCcw, Send } from 'lucide-react'
import {
  getConfirmationCaseDetail,
  sendNewTimeWindow,
} from '@/api/confirmation-cases-api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import type {
  CommunicationChannel,
  NewTimeWindowRequest,
  ConfirmationCaseDetail,
} from '@/types/confirmation-cases'
import { HistoryCard } from './history-card'
import { RequestContextCard } from './request-context-card'
import { SummaryCard } from './summary-card'
import { ChannelButton, PageShell } from './shared'
import {
  formatDateInputValue,
  formatDateTime,
  formatTimeInputValue,
  inputClassName,
  parseLocalDateTime,
} from '../utils'
import type { PageStatus } from '../utils'

export function NewTimeWindowPage({
  navigate,
  orderId,
}: {
  navigate: (path: string) => void
  orderId: string
}) {
  const [status, setStatus] = useState<PageStatus>('loading')
  const [confirmationCase, setConfirmationCase] =
    useState<ConfirmationCaseDetail | null>(null)
  const [channel, setChannel] = useState<CommunicationChannel>('email')
  const [deliveryDate, setDeliveryDate] = useState('')
  const [deliveryWindowStart, setDeliveryWindowStart] = useState('')
  const [deliveryWindowEnd, setDeliveryWindowEnd] = useState('')
  const [responseDeadlineHours, setResponseDeadlineHours] = useState('12')
  const [formError, setFormError] = useState<string | null>(null)

  useEffect(() => {
    async function loadConfirmationCase() {
      try {
        const detail = await getConfirmationCaseDetail(orderId)

        setConfirmationCase(detail)
        setChannel(detail.previousRequest.channel)
        setDeliveryDate(detail.recommendedDeliveryDate)
        setDeliveryWindowStart(detail.recommendedWindowStart.slice(0, 5))
        setDeliveryWindowEnd(detail.recommendedWindowEnd.slice(0, 5))
        setResponseDeadlineHours(String(detail.defaultResponseDeadlineHours))
        setStatus('ready')
      } catch {
        setStatus('error')
      }
    }

    void loadConfirmationCase()
  }, [orderId])

  const deadlineWarning = useMemo(() => {
    const hours = Number(responseDeadlineHours)

    if (
      !deliveryDate ||
      !deliveryWindowStart ||
      Number.isNaN(hours) ||
      hours < 1
    ) {
      return null
    }

    const deliveryStart = parseLocalDateTime(
      deliveryDate,
      `${deliveryWindowStart}:00`,
    )

    if (deliveryStart === null) {
      return null
    }

    const deadlineAt = new Date()
    deadlineAt.setMinutes(0, 0, 0)
    deadlineAt.setHours(deadlineAt.getHours() + hours)

    if (deadlineAt <= deliveryStart) {
      return null
    }

    return {
      deadlineAt,
      deliveryStart,
    }
  }, [deliveryDate, deliveryWindowStart, responseDeadlineHours])

  const now = new Date()
  const minDeliveryDate = formatDateInputValue(now)
  const minDeliveryTime =
    deliveryDate === minDeliveryDate ? formatTimeInputValue(now) : undefined
  const selectedDeliveryStart = parseLocalDateTime(
    deliveryDate,
    `${deliveryWindowStart || '00:00'}:00`,
  )
  const isScheduledInPast =
    deliveryDate !== '' &&
    deliveryWindowStart !== '' &&
    selectedDeliveryStart !== null &&
    selectedDeliveryStart < now

  async function handleSubmit() {
    if (isScheduledInPast) {
      setFormError(
        'Der Tourstart darf nicht vor dem aktuellen Datum und der aktuellen Uhrzeit liegen.',
      )
      return
    }

    if (deliveryWindowEnd <= deliveryWindowStart) {
      setFormError('Das Ende des Zeitfensters muss nach dem Start liegen.')
      return
    }

    if (Number(responseDeadlineHours) < 1) {
      setFormError('Die Antwortfrist muss mindestens 1 Stunde betragen.')
      return
    }

    setFormError(null)
    setStatus('submitting')

    const request: NewTimeWindowRequest = {
      channel,
      deliveryDate,
      deliveryWindowStart: `${deliveryWindowStart}:00`,
      deliveryWindowEnd: `${deliveryWindowEnd}:00`,
      responseDeadlineHours: Number(responseDeadlineHours),
    }

    try {
      await sendNewTimeWindow(orderId, request)
      setStatus('success')
      window.setTimeout(() => navigate('/dashboard'), 1000)
    } catch {
      setStatus('ready')
      setFormError('Die neue Anfrage konnte nicht versendet werden.')
    }
  }

  if (status === 'loading') {
    return (
      <PageShell
        title="Neues Zeitfenster senden"
        description="Auftragsdaten werden geladen."
      >
        <Card className="mx-auto w-full max-w-6xl rounded-3xl border shadow-sm">
          <CardContent className="px-6 py-12 text-center text-muted-foreground">
            Auftragsdetails werden geladen...
          </CardContent>
        </Card>
      </PageShell>
    )
  }

  if (status === 'error' || confirmationCase === null) {
    return (
      <PageShell
        title="Neues Zeitfenster senden"
        description="Der Auftrag konnte nicht geöffnet werden."
      >
        <Card className="mx-auto w-full max-w-6xl rounded-3xl border shadow-sm">
          <CardContent className="px-6 py-12">
            <Alert className="border-red-300 bg-red-50">
              <AlertDescription className="text-red-950">
                Der gewählte Problemfall wurde nicht gefunden oder die API ist
                derzeit nicht erreichbar.
              </AlertDescription>
            </Alert>
          </CardContent>
        </Card>
      </PageShell>
    )
  }

  const isSubmitting = status === 'submitting'
  const isSuccess = status === 'success'

  return (
    <PageShell
      topAccessory={
        <Button
          className="w-fit rounded-full border-slate-200 px-4 text-slate-700 hover:bg-slate-100"
          type="button"
          variant="outline"
          onClick={() => navigate('/dashboard')}
        >
          <ArrowLeft className="size-4" />
          Zurück zum Dashboard
        </Button>
      }
      title="Neues Zeitfenster senden"
      description="Passen Sie das Lieferfenster an und versenden Sie eine neue Rueckbestaetigung im gleichen Stil wie die bestehende Kundenansicht."
    >
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-6">
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(340px,0.92fr)]">
          <div className="grid gap-5">
            <SummaryCard confirmationCase={confirmationCase} />
            <RequestContextCard confirmationCase={confirmationCase} />
          </div>

          <Card className="gap-0 rounded-3xl border border-slate-200 bg-white shadow-sm xl:sticky xl:top-6">
            <CardHeader className="rounded-t-3xl border-b border-slate-100 bg-slate-50/80 px-6 py-5">
              <CardTitle className="flex items-center gap-2 text-slate-950">
                <RotateCcw className="size-4 text-slate-600" />
                Neue Anfrage
              </CardTitle>
              <CardDescription className="text-slate-600">
                Neues Lieferfenster, Versandkanal und Antwortfrist festlegen.
              </CardDescription>
            </CardHeader>
            <CardContent className="grid gap-5 px-6 py-6">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">
                  Versandlogik
                </p>
                <p className="mt-2 text-sm text-slate-700">
                  Die vorherige Anfrage wird beim Versand automatisch
                  deaktiviert. Der Nachrichtentext wird systemseitig erzeugt.
                </p>
              </div>

              <div className="grid gap-2">
                <label className="text-[11px] font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  Kommunikationskanal
                </label>
                <div className="grid grid-cols-3 gap-2">
                  <ChannelButton
                    active={channel === 'email'}
                    icon={<Mail className="size-4" />}
                    label="E-Mail"
                    onClick={() => setChannel('email')}
                  />
                  <ChannelButton
                    active={channel === 'sms'}
                    icon={<Phone className="size-4" />}
                    label="SMS"
                    onClick={() => setChannel('sms')}
                  />
                  <ChannelButton
                    active={channel === 'whatsapp'}
                    icon={<MessageCircle className="size-4" />}
                    label="WhatsApp"
                    onClick={() => setChannel('whatsapp')}
                  />
                </div>
              </div>

              <div className="grid gap-2">
                <label
                  className="text-[11px] font-semibold uppercase tracking-[0.18em] text-muted-foreground"
                  htmlFor="delivery-date"
                >
                  Neues Lieferdatum
                </label>
                <input
                  id="delivery-date"
                  className={inputClassName}
                  min={minDeliveryDate}
                  type="date"
                  value={deliveryDate}
                  onChange={(event) => setDeliveryDate(event.target.value)}
                />
              </div>

              <div className="grid gap-2">
                <label className="text-[11px] font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  Neues Zeitfenster
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <input
                    aria-label="Zeitfenster Start"
                    className={inputClassName}
                    min={minDeliveryTime}
                    type="time"
                    value={deliveryWindowStart}
                    onChange={(event) =>
                      setDeliveryWindowStart(event.target.value)
                    }
                  />
                  <input
                    aria-label="Zeitfenster Ende"
                    className={inputClassName}
                    type="time"
                    value={deliveryWindowEnd}
                    onChange={(event) => setDeliveryWindowEnd(event.target.value)}
                  />
                </div>
              </div>

              <div className="grid gap-2">
                <label
                  className="text-[11px] font-semibold uppercase tracking-[0.18em] text-muted-foreground"
                  htmlFor="deadline-hours"
                >
                  Antwortfrist in Stunden
                </label>
                <input
                  id="deadline-hours"
                  className={inputClassName}
                  min="1"
                  type="number"
                  value={responseDeadlineHours}
                  onChange={(event) =>
                    setResponseDeadlineHours(event.target.value)
                  }
                />
                <p className="text-xs text-slate-500">
                  Die Frist wird ab erfolgreichem Versand neu berechnet.
                </p>
              </div>

              {deadlineWarning ? (
                <Alert className="border-amber-200 bg-amber-50/80">
                  <AlertDescription className="text-amber-900">
                    Die Antwortfrist endet voraussichtlich am{' '}
                    {formatDateTime(deadlineWarning.deadlineAt.toISOString())}
                    . Das liegt nach dem gewaehlten Lieferbeginn am{' '}
                    {formatDateTime(
                      deadlineWarning.deliveryStart.toISOString(),
                    )}
                    .
                  </AlertDescription>
                </Alert>
              ) : null}

              {isScheduledInPast ? (
                <Alert className="border-red-200 bg-red-50/80">
                  <AlertDescription className="text-red-900">
                    Der gewaehlte Tourstart liegt vor dem aktuellen Datum oder
                    der aktuellen Uhrzeit. Bitte waehlen Sie einen spaeteren
                    Zeitpunkt.
                  </AlertDescription>
                </Alert>
              ) : null}

              <Alert className="border-amber-200 bg-amber-50/80">
                <AlertDescription className="text-amber-900">
                  Wenn die Antwortfrist hinter dem Beginn des Zeitfensters
                  liegt, sollte das Backend sie beim Versand begrenzen.
                </AlertDescription>
              </Alert>

              {formError ? (
                <Alert className="border-red-300 bg-red-50">
                  <AlertDescription className="text-red-950">
                    {formError}
                  </AlertDescription>
                </Alert>
              ) : null}

              {isSuccess ? (
                <Alert className="border-green-300 bg-green-50">
                  <AlertDescription className="text-green-950">
                    Neue Rückbestätigung wurde versendet. Wir leiten gleich
                    zurück zur Problemfall-Übersicht.
                  </AlertDescription>
                </Alert>
              ) : null}

              <div className="grid gap-3 border-t border-slate-100 pt-5">
                <Button
                  className="h-12 rounded-2xl bg-slate-900 text-sm shadow-sm hover:bg-slate-800"
                  disabled={isSubmitting || isSuccess}
                  type="button"
                  onClick={() => void handleSubmit()}
                >
                  {isSuccess ? (
                    <>
                      <CheckCircle2 className="size-4" />
                      Rückbestätigung gesendet
                    </>
                  ) : (
                    <>
                      <Send className="size-4" />
                      Neues Zeitfenster senden
                    </>
                  )}
                </Button>
                <Button
                  className="h-12 rounded-2xl border-slate-200 text-slate-700 hover:bg-slate-50"
                  disabled={isSubmitting}
                  type="button"
                  variant="outline"
                  onClick={() => navigate('/')}
                >
                  Abbrechen
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>

        <HistoryCard history={confirmationCase.history} />
      </div>
    </PageShell>
  )
}
