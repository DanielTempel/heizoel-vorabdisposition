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
    className:
      'border-slate-200 bg-slate-50 text-slate-800 dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-200',
  },
  PENDING: {
    label: 'Wird versendet',
    className:
      'border-violet-200 bg-violet-50 text-violet-800 dark:border-violet-800 dark:bg-violet-950/40 dark:text-violet-300',
  },
  SENT: {
    label: 'Versendet',
    className:
      'border-blue-200 bg-blue-50 text-blue-800 dark:border-blue-800 dark:bg-blue-950/40 dark:text-blue-300',
  },
  CONFIRMED: {
    label: 'Bestätigt',
    className:
      'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300',
  },
  REJECTED: {
    label: 'Abgelehnt',
    className:
      'border-red-200 bg-red-50 text-red-800 dark:border-red-800 dark:bg-red-950/40 dark:text-red-300',
  },
  NO_RESPONSE: {
    label: 'Keine Rückmeldung',
    className:
      'border-amber-200 bg-amber-50 text-amber-900 dark:border-amber-800 dark:bg-amber-950/40 dark:text-amber-300',
  },
  FAILED: {
    label: 'Versand fehlgeschlagen',
    className:
      'border-rose-300 bg-rose-50 text-rose-900 dark:border-rose-800 dark:bg-rose-950/40 dark:text-rose-300',
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
