import { cn } from '@/lib/utils'
import type { ConfirmationDisplayStatus } from '@/types/confirmation'

type ConfirmationStatusBadgeProps = {
  status: ConfirmationDisplayStatus
  count?: number
  className?: string
}

const statusPresentation = {
  OPEN: {
    label: 'Offen',
    className: 'border-slate-200 bg-slate-50 text-slate-800',
  },
  PENDING: {
    label: 'Wird versendet',
    className: 'border-violet-200 bg-violet-50 text-violet-800',
  },
  SENT: {
    label: 'Versendet',
    className: 'border-blue-200 bg-blue-50 text-blue-800',
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
    className: 'border-amber-200 bg-amber-50 text-amber-900',
  },
  FAILED: {
    label: 'Versand fehlgeschlagen',
    className: 'border-rose-300 bg-rose-50 text-rose-900',
  },
} satisfies Record<
  ConfirmationDisplayStatus,
  { label: string; className: string }
>

export function ConfirmationStatusBadge({
  status,
  count,
  className,
}: ConfirmationStatusBadgeProps) {
  const presentation = statusPresentation[status]

  return (
    <span
      className={cn(
        'inline-flex rounded-full border px-2 py-0.5 text-xs font-medium',
        presentation.className,
        className,
      )}
      data-status={status}
    >
      {presentation.label}
      {count === undefined ? null : `: ${count}`}
    </span>
  )
}
