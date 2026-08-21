import { useEffect, useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, getOrderDetail } from '@/api/dashboard-api'
import { ConfirmationStatusBadge } from '@/components/confirmation-status-badge'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import type { OrderDetail } from '@/types/dashboard'
import { RequestCard } from './components/request-card'
import { ResendConfirmationCard } from './components/resend-confirmation-card'

type PageStatus = 'loading' | 'ready' | 'not-found' | 'error'

function DetailField({
  label,
  value,
}: {
  label: string
  value: string
}) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="mt-0.5 text-sm font-medium">{value}</dd>
    </div>
  )
}

function getErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) {
    return 'Die Auftragsdetails konnten nicht geladen werden.'
  }

  if (error.status === 401) {
    return 'Die Dashboard-Sitzung ist abgelaufen oder nicht mehr gültig.'
  }

  if (error.status === 403) {
    return 'Für diesen Auftrag ist kein Zugriff möglich.'
  }

  return 'Die Auftragsdetails konnten nicht geladen werden.'
}

export function OrderDetailPage() {
  const { externalOrderId } = useParams()
  const [reloadKey, setReloadKey] = useState(0)
  const [status, setStatus] = useState<PageStatus>('loading')
  const [errorMessage, setErrorMessage] = useState('')
  const [detail, setDetail] = useState<OrderDetail | null>(null)

  useEffect(() => {
    if (externalOrderId === undefined) {
      return
    }

    const controller = new AbortController()
    const orderId = externalOrderId

    async function loadOrderDetail() {
      try {
        const nextDetail = await getOrderDetail(orderId, controller.signal)

        if (!controller.signal.aborted) {
          setDetail(nextDetail)
          setStatus('ready')
        }
      } catch (error) {
        if (controller.signal.aborted) {
          return
        }

        if (error instanceof ApiError && error.status === 404) {
          setStatus('not-found')
          return
        }

        setErrorMessage(getErrorMessage(error))
        setStatus('error')
      }
    }

    void loadOrderDetail()

    return () => controller.abort()
  }, [externalOrderId, reloadKey])

  function reload() {
    setStatus('loading')
    refreshDetail()
  }

  function refreshDetail() {
    setReloadKey((currentKey) => currentKey + 1)
  }

  const order = detail?.order

  return (
    <section className="grid gap-6" aria-labelledby="order-detail-title">
      <div>
        <Button asChild variant="outline">
          <Link to="/dashboard">
            <ArrowLeft />
            Zurück zur Tourübersicht
          </Link>
        </Button>
      </div>

      <div>
        <p className="text-sm text-muted-foreground">Auftragsdetails</p>
        <div className="mt-1 flex flex-wrap items-center gap-3">
          <h2 className="text-2xl font-semibold" id="order-detail-title">
            Auftrag {order?.externalOrderId ?? externalOrderId}
          </h2>
          {order === undefined ? null : (
            <ConfirmationStatusBadge status={order.confirmationStatus} />
          )}
        </div>
      </div>

      {status === 'loading' ? (
        <div
          aria-live="polite"
          className="rounded-lg border border-dashed bg-background p-10 text-center text-sm text-muted-foreground"
        >
          Auftragsdetails werden geladen…
        </div>
      ) : null}

      {status === 'not-found' ? (
        <Alert variant="destructive">
          <AlertDescription>
            Der Auftrag wurde nicht gefunden oder ist für dieses Unternehmen
            nicht verfügbar.
          </AlertDescription>
        </Alert>
      ) : null}

      {status === 'error' ? (
        <Alert variant="destructive">
          <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
            <span>{errorMessage}</span>
            <Button onClick={reload} size="sm" variant="outline">
              Erneut versuchen
            </Button>
          </AlertDescription>
        </Alert>
      ) : null}

      {status === 'ready' && detail && order ? (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_22rem] lg:items-start">
          <div className="grid gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Auftragsinformationen</CardTitle>
              </CardHeader>
              <CardContent>
                <dl className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
                  <DetailField label="Kunde" value={order.customerName} />
                  <DetailField
                    label="E-Mail"
                    value={order.customerEmail ?? '–'}
                  />
                  <DetailField
                    label="Telefon"
                    value={order.customerPhoneNumber ?? '–'}
                  />
                  <DetailField
                    label="Lieferadresse"
                    value={order.deliveryAddress}
                  />
                  <DetailField label="Tour" value={order.tourNumber} />
                  <DetailField
                    label="Fahrzeug"
                    value={order.vehicleLicensePlate || '–'}
                  />
                  <DetailField label="Produkt" value={order.product} />
                  <DetailField
                    label="Menge"
                    value={`${new Intl.NumberFormat('de-DE').format(order.quantityLiters)} Liter`}
                  />
                  <DetailField
                    label="Preis"
                    value={order.priceDisplayText ?? '–'}
                  />
                </dl>
              </CardContent>
            </Card>

            <section
              className="grid gap-3"
              aria-labelledby="current-request-title"
            >
              <h3 className="text-lg font-semibold" id="current-request-title">
                Aktuelle Anfrage
              </h3>
              {detail.currentRequest === null ? (
                <div className="rounded-lg border border-dashed bg-background p-8 text-center text-sm text-muted-foreground">
                  Keine aktuelle Avisierungsanfrage vorhanden.
                </div>
              ) : (
                <RequestCard request={detail.currentRequest} />
              )}
            </section>

            <section
              className="grid gap-3"
              aria-labelledby="request-history-title"
            >
              <h3 className="text-lg font-semibold" id="request-history-title">
                Frühere Anfragen
              </h3>
              {detail.previousRequests.length === 0 ? (
                <div className="rounded-lg border border-dashed bg-background p-8 text-center text-sm text-muted-foreground">
                  Keine früheren Avisierungsanfragen vorhanden.
                </div>
              ) : (
                <div className="grid gap-3">
                  {detail.previousRequests.map((request) => (
                    <RequestCard key={request.requestId} request={request} />
                  ))}
                </div>
              )}
            </section>
          </div>

          <aside aria-label="Aktionen" className="lg:sticky lg:top-6">
            <ResendConfirmationCard
              detail={detail}
              onSuccess={refreshDetail}
            />
          </aside>
        </div>
      ) : null}
    </section>
  )
}
