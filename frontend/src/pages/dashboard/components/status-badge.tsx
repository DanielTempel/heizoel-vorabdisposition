import type { ConfirmationStatus } from '../../../types/confirmation'
import { cn } from '@/lib/utils'

type StatusBadgeProps = {
  status: ConfirmationStatus
}

const statusConfig = {
  SENT: {
    label: 'Offen',
    className: 'border-amber-200 bg-amber-50 text-amber-800',
  },
  CONFIRMED: {
    label: 'Bestätigt',
    className: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  },
  REJECTED: {
    label: 'Abgelehnt',
    className: 'border-red-200 bg-red-50 text-red-800',
  },
  NO_RESPONSE: {
    label: 'Keine Rückmeldung',
    className: 'border-stone-200 bg-stone-100 text-stone-700',
  },
} satisfies Record<ConfirmationStatus, { label: string; className: string }>

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status]

  return (
    <span
      className={cn(
        'inline-flex rounded-full border px-2.5 py-1 text-xs font-medium',
        config.className,
      )}
    >
      {config.label}
    </span>
  )
}

