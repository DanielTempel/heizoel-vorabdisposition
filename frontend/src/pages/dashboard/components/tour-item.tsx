import { useId, useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { Link } from 'react-router-dom'
import { ConfirmationStatusBadge } from '@/components/confirmation-status-badge'
import {
  Card,
  CardContent,
  CardHeader,
} from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { cn } from '@/lib/utils'
import type { TourSummary } from '@/types/dashboard'

type TourItemProps = {
  tour: TourSummary
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
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatTime(value: string) {
  return value.slice(0, 5)
}

export function TourItem({ tour }: TourItemProps) {
  const [isOpen, setIsOpen] = useState(false)
  const tableId = useId()

  return (
    <Card className="gap-0 py-0">
      <CardHeader className="p-0">
        <button
          aria-controls={tableId}
          aria-expanded={isOpen}
          className="grid w-full cursor-pointer gap-4 p-4 text-left transition-colors hover:bg-muted/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center"
          onClick={() => setIsOpen((currentValue) => !currentValue)}
          type="button"
        >
          <span className="grid gap-3 sm:grid-cols-4">
            <span>
              <span className="block text-xs text-muted-foreground">Tour</span>
              <strong className="block text-base">{tour.tourNumber}</strong>
            </span>
            <span>
              <span className="block text-xs text-muted-foreground">
                Lieferdatum
              </span>
              <strong className="block text-sm">{formatDate(tour.deliveryDate)}</strong>
            </span>
            <span>
              <span className="block text-xs text-muted-foreground">
                Fahrzeug
              </span>
              <strong className="block text-sm">
                {tour.vehicleLicensePlate || '–'}
              </strong>
            </span>
            <span>
              <span className="block text-xs text-muted-foreground">
                Aufträge
              </span>
              <strong className="block text-sm">{tour.orders.length}</strong>
            </span>
          </span>

          <span className="flex items-center justify-between gap-3 sm:justify-end">
            <span className="flex flex-wrap gap-1.5">
              <ConfirmationStatusBadge
                count={tour.statusCounts.sent}
                status="SENT"
              />
              <ConfirmationStatusBadge
                count={tour.statusCounts.confirmed}
                status="CONFIRMED"
              />
              <ConfirmationStatusBadge
                count={tour.statusCounts.rejected}
                status="REJECTED"
              />
              <ConfirmationStatusBadge
                count={tour.statusCounts.noResponse}
                status="NO_RESPONSE"
              />
            </span>
            <ChevronDown
              aria-hidden="true"
              className={cn(
                'size-5 shrink-0 text-muted-foreground transition-transform',
                isOpen && 'rotate-180',
              )}
            />
          </span>
        </button>
      </CardHeader>

      {isOpen ? (
        <CardContent className="border-t p-0" id={tableId}>
          {tour.orders.length === 0 ? (
            <p className="p-6 text-center text-muted-foreground">
              Für diese Tour sind keine Aufträge vorhanden.
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Auftrag</TableHead>
                  <TableHead>Kunde</TableHead>
                  <TableHead>Adresse</TableHead>
                  <TableHead>Zeitfenster</TableHead>
                  <TableHead>Antwortfrist</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {tour.orders.map((order) => (
                  <TableRow
                    className="relative cursor-pointer hover:bg-muted/50"
                    key={order.externalOrderId}
                  >
                    <TableCell>
                      <Link
                        aria-label={`Auftrag ${order.externalOrderId} öffnen`}
                        className="font-medium outline-none after:absolute after:inset-0 after:content-[''] focus-visible:after:ring-2 focus-visible:after:ring-ring"
                        to={`/dashboard/orders/${encodeURIComponent(order.externalOrderId)}`}
                      >
                        {order.externalOrderId}
                      </Link>
                    </TableCell>
                    <TableCell>{order.customerName}</TableCell>
                    <TableCell className="max-w-72 whitespace-normal">
                      {order.deliveryAddress}
                    </TableCell>
                    <TableCell>
                      {formatTime(order.deliveryWindowStart)}–
                      {formatTime(order.deliveryWindowEnd)} Uhr
                    </TableCell>
                    <TableCell>{formatDateTime(order.expiresAt)}</TableCell>
                    <TableCell>
                      <ConfirmationStatusBadge
                        status={order.confirmationStatus}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      ) : null}
    </Card>
  )
}
