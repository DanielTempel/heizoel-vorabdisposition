import { MapPin } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { ConfirmationCaseDetail } from '@/types/confirmation-cases'
import { ConfirmationCaseBadge, DetailGridItem } from './shared'
import { formatDate, formatTime } from '../utils'

export function SummaryCard({
  confirmationCase,
}: {
  confirmationCase: ConfirmationCaseDetail
}) {
  return (
    <Card className="rounded-3xl border border-slate-200 bg-white shadow-sm">
      <CardHeader className="border-b border-slate-100 px-6 py-5">
        <div className="flex items-center justify-between gap-4">
          <CardTitle>Aktueller Auftrag</CardTitle>
          <ConfirmationCaseBadge type={confirmationCase.problemType} />
        </div>
      </CardHeader>
      <CardContent className="grid gap-4 px-6 py-6">
        <section className="rounded-2xl border border-slate-200 bg-slate-50 px-5 py-5">
          <p className="text-xs font-semibold uppercase text-slate-500">
            Aktuelles Lieferfenster
          </p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-950">
            {formatDate(confirmationCase.deliveryDate)}
          </h2>
          <p className="mt-2 text-base font-medium text-slate-700">
            {formatTime(confirmationCase.deliveryWindowStart)} -{' '}
            {formatTime(confirmationCase.deliveryWindowEnd)} Uhr
          </p>
        </section>
        <div className="grid gap-4 sm:grid-cols-2">
          <DetailGridItem label="Auftrag" value={confirmationCase.orderId} />
          <DetailGridItem label="Kunde" value={confirmationCase.customerName} />
        </div>
        <DetailGridItem
          icon={<MapPin className="size-3.5 text-muted-foreground" />}
          label="Lieferadresse"
          value={confirmationCase.deliveryAddress}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <DetailGridItem label="Produkt" value={confirmationCase.product} />
          <DetailGridItem
            label="Menge"
            value={`${confirmationCase.quantityLiters.toLocaleString('de-DE')} Liter`}
          />
        </div>
        {confirmationCase.priceDisplayText ? (
          <DetailGridItem label="Preis" value={confirmationCase.priceDisplayText} />
        ) : null}
      </CardContent>
    </Card>
  )
}
