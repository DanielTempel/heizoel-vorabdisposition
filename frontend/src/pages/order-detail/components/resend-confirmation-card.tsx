import { useId, useState, type FormEvent } from 'react'
import { LoaderCircle, Send } from 'lucide-react'
import { ApiError, resendConfirmation } from '@/api/dashboard-api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import type {
  CommunicationChannel,
  OrderDetail,
} from '@/types/dashboard'

type ResendConfirmationCardProps = {
  detail: OrderDetail
  onSuccess: () => void
}

const channelOptions: Array<{
  value: CommunicationChannel
  label: string
}> = [
  { value: 'EMAIL', label: 'E-Mail' },
  { value: 'SMS', label: 'SMS' },
  { value: 'WHATSAPP', label: 'WhatsApp' },
]

function hasValue(value: string | null) {
  return value !== null && value.trim() !== ''
}

function isChannelAvailable(
  channel: CommunicationChannel,
  detail: OrderDetail,
) {
  if (channel === 'EMAIL') {
    return hasValue(detail.order.customerEmail)
  }

  return hasValue(detail.order.customerPhoneNumber)
}

function getInitialChannel(detail: OrderDetail): CommunicationChannel {
  const currentChannel = detail.currentRequest?.communicationChannel

  if (currentChannel && isChannelAvailable(currentChannel, detail)) {
    return currentChannel
  }

  return (
    channelOptions.find((option) =>
      isChannelAvailable(option.value, detail),
    )?.value ?? 'EMAIL'
  )
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'medium',
  }).format(new Date(`${value}T00:00:00`))
}

function formatTime(value: string) {
  return value.slice(0, 5)
}

function getErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) {
    return 'Die neue Anfrage konnte nicht erstellt werden.'
  }

  if (error.code === 'CONFIRMATION_REQUEST_DELIVERY_IN_PROGRESS') {
    return 'Eine Anfrage wird bereits versendet. Bitte warten Sie, bis der Versand abgeschlossen ist.'
  }

  if (error.code === 'MISSING_DIGITAL_CONTACT') {
    return 'Für den ausgewählten Kanal fehlen die Kontaktdaten des Kunden.'
  }

  if (error.status === 401) {
    return 'Die Dashboard-Sitzung ist abgelaufen oder nicht mehr gültig.'
  }

  return 'Die neue Anfrage konnte nicht erstellt werden.'
}

export function ResendConfirmationCard({
  detail,
  onSuccess,
}: ResendConfirmationCardProps) {
  const deadlineInputId = useId()
  const [channel, setChannel] = useState<CommunicationChannel>(() =>
    getInitialChannel(detail),
  )
  const [deadlineHours, setDeadlineHours] = useState(
    String(detail.currentRequest?.responseDeadlineHours ?? 24),
  )
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const parsedDeadlineHours = Number(deadlineHours)
  const isDeadlineValid =
    Number.isInteger(parsedDeadlineHours) &&
    parsedDeadlineHours >= 1 &&
    parsedDeadlineHours <= 168
  const hasCurrentRequest = detail.currentRequest !== null
  const isDeliveryPending = detail.currentRequest?.status === 'PENDING'
  const controlsDisabled = isSubmitting || isDeliveryPending
  const selectedChannelAvailable = isChannelAvailable(channel, detail)
  const canSubmit =
    hasCurrentRequest &&
    !isDeliveryPending &&
    selectedChannelAvailable &&
    isDeadlineValid &&
    !isSubmitting

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!canSubmit) {
      return
    }

    setIsSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      await resendConfirmation(detail.order.externalOrderId, {
        communicationChannel: channel,
        responseDeadlineHours: parsedDeadlineHours,
      })

      setSuccessMessage('Die neue Anfrage wurde erstellt.')
      onSuccess()
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Neue Anfrage</CardTitle>
      </CardHeader>

      <CardContent>
        {detail.currentRequest === null ? (
          <Alert>
            <AlertDescription>
              Ohne vorhandene Anfrage ist kein erneuter Versand möglich.
            </AlertDescription>
          </Alert>
        ) : (
          <form className="grid gap-4" onSubmit={submit}>
            <div className="rounded-md border bg-muted/30 p-3">
              <p className="text-xs text-muted-foreground">
                Vorgeschlagener Liefertermin
              </p>
              <p className="mt-1 text-sm font-medium">
                {formatDate(detail.currentRequest.deliveryDate)} ·{' '}
                {formatTime(detail.currentRequest.deliveryWindowStart)}–
                {formatTime(detail.currentRequest.deliveryWindowEnd)} Uhr
              </p>
            </div>

            <fieldset className="grid gap-2" disabled={controlsDisabled}>
              <legend className="text-xs font-medium">
                Kommunikationskanal
              </legend>
              <div className="grid grid-cols-3 gap-2">
                {channelOptions.map((option) => {
                  const available = isChannelAvailable(option.value, detail)

                  return (
                    <Button
                      aria-pressed={channel === option.value}
                      disabled={!available}
                      key={option.value}
                      onClick={() => setChannel(option.value)}
                      type="button"
                      variant={channel === option.value ? 'default' : 'outline'}
                    >
                      {option.label}
                    </Button>
                  )
                })}
              </div>
              {!hasValue(detail.order.customerEmail) ? (
                <p className="text-xs text-muted-foreground">
                  Keine E-Mail-Adresse hinterlegt.
                </p>
              ) : null}
              {!hasValue(detail.order.customerPhoneNumber) ? (
                <p className="text-xs text-muted-foreground">
                  Keine Telefonnummer hinterlegt.
                </p>
              ) : null}
            </fieldset>

            <div className="grid gap-1.5">
              <label className="text-xs font-medium" htmlFor={deadlineInputId}>
                Neue Antwortfrist (Stunden)
              </label>
              <Input
                aria-describedby={`${deadlineInputId}-description`}
                aria-invalid={!isDeadlineValid}
                disabled={controlsDisabled}
                id={deadlineInputId}
                max={168}
                min={1}
                onChange={(event) => setDeadlineHours(event.target.value)}
                step={1}
                type="number"
                value={deadlineHours}
              />
              <p
                className="text-xs text-muted-foreground"
                id={`${deadlineInputId}-description`}
              >
                1 bis 168 Stunden, spätestens bis zum Liefertermin.
              </p>
            </div>

            {isDeliveryPending ? (
              <Alert>
                <AlertDescription>
                  Der Versand läuft bereits.
                </AlertDescription>
              </Alert>
            ) : null}

            {errorMessage === '' ? null : (
              <Alert variant="destructive">
                <AlertDescription>{errorMessage}</AlertDescription>
              </Alert>
            )}

            {successMessage === '' ? null : (
              <Alert className="border-emerald-300 bg-emerald-50 text-emerald-950">
                <AlertDescription className="text-emerald-950">
                  {successMessage}
                </AlertDescription>
              </Alert>
            )}

            <Button className="w-full" disabled={!canSubmit} type="submit">
              {isSubmitting ? (
                <LoaderCircle className="animate-spin" />
              ) : (
                <Send />
              )}
              {isSubmitting ? 'Anfrage wird erstellt…' : 'Erneut senden'}
            </Button>
          </form>
        )}
      </CardContent>
    </Card>
  )
}
