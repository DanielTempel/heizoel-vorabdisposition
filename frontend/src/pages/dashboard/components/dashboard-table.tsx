import { Eye } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatDate, formatTime } from '../../../lib/format-delivery'
import type { DashboardConfirmation } from '../../../types/dashboard'
import { StatusBadge } from './status-badge'

type DashboardTableProps = {
  confirmations: DashboardConfirmation[]
}

function formatDeadline(confirmation: DashboardConfirmation) {
  if (
    confirmation.confirmationStatus === 'CONFIRMED' ||
    confirmation.confirmationStatus === 'REJECTED'
  ) {
    return '-'
  }

  if (confirmation.expiresAt === null) {
    return '-'
  }

  const expiresAt = new Date(confirmation.expiresAt)
  const now = new Date()
  const remainingHours = Math.ceil(
    (expiresAt.getTime() - now.getTime()) / (1000 * 60 * 60),
  )

  if (remainingHours <= 0) {
    return 'Abgelaufen'
  }

  if (remainingHours <= 6) {
    return `${remainingHours}h übrig`
  }

  return `${remainingHours}h übrig`
}

export function DashboardTable({ confirmations }: DashboardTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Auftrag</TableHead>
          <TableHead>Kunde</TableHead>
          <TableHead>Lieferdatum</TableHead>
          <TableHead>Zeitfenster</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Frist</TableHead>
          <TableHead className="text-right">Aktionen</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {confirmations.map((confirmation) => (
          <TableRow key={confirmation.externalOrderId}>
            <TableCell className="font-medium">
              {confirmation.externalOrderId}
            </TableCell>
            <TableCell>
              {confirmation.customerName}
            </TableCell>
            <TableCell>{formatDate(confirmation.deliveryDate)}</TableCell>
            <TableCell>
              {formatTime(confirmation.deliveryWindowStart)} -{' '}
              {formatTime(confirmation.deliveryWindowEnd)} Uhr
            </TableCell>
            <TableCell>
              <StatusBadge status={confirmation.confirmationStatus} />
            </TableCell>
            <TableCell>{formatDeadline(confirmation)}</TableCell>
            <TableCell className="text-right">
              <Button size="sm" variant="outline" type="button">
                <Eye className="size-3" />
                Details
              </Button>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
