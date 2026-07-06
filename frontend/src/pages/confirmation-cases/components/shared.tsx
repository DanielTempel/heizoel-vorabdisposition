import type { ReactNode } from 'react'
import { AlertCircle, XCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import type { ConfirmationCaseType } from '@/types/confirmation-cases'
import { channelLabel, confirmationCaseLabel } from '../utils'

export function PageShell({
  topAccessory,
  title,
  description,
  action,
  children,
}: {
  topAccessory?: ReactNode
  title: string
  description: string
  action?: ReactNode
  children: ReactNode
}) {
  return (
    <main className="min-h-screen bg-background px-4 py-6 text-foreground sm:px-6 lg:px-8">
      {topAccessory ? (
        <div className="mx-auto mb-4 flex w-full max-w-6xl">{topAccessory}</div>
      ) : null}
      <div className="mx-auto mb-6 flex w-full max-w-6xl flex-col gap-4 rounded-3xl border border-slate-200 bg-white px-6 py-6 shadow-sm sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-500">
            Lieferbestaetigung
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">
            {title}
          </h1>
          <p className="mt-2 max-w-3xl text-sm text-slate-600">{description}</p>
        </div>
        {action}
      </div>
      {children}
    </main>
  )
}

export function StatCard({
  icon,
  label,
  value,
  tone,
  hint,
}: {
  icon: ReactNode
  label: string
  value: number
  tone: 'default' | 'danger' | 'warning'
  hint: string
}) {
  return (
    <Card
      className={cn(
        'rounded-3xl border shadow-sm',
        tone === 'danger' && 'border-red-200',
        tone === 'warning' && 'border-amber-200',
      )}
    >
      <CardContent className="px-6 py-6">
        <div className="flex items-center justify-between">
          <p
            className={cn(
              'text-sm font-medium text-slate-600',
              tone === 'danger' && 'text-red-700',
              tone === 'warning' && 'text-amber-700',
            )}
          >
            {label}
          </p>
          {icon}
        </div>
        <p className="mt-3 text-3xl font-semibold text-slate-950">{value}</p>
        <p className="mt-1 text-xs text-muted-foreground">{hint}</p>
      </CardContent>
    </Card>
  )
}

export function FilterButton({
  active,
  label,
  onClick,
  tone = 'default',
}: {
  active: boolean
  label: string
  onClick: () => void
  tone?: 'default' | 'danger' | 'warning'
}) {
  return (
    <Button
      className={cn(
        'h-10 rounded-full px-4 text-sm',
        active && tone === 'default' && 'bg-slate-900 hover:bg-slate-800',
        active && tone === 'danger' && 'bg-red-600 hover:bg-red-500',
        active &&
          tone === 'warning' &&
          'bg-amber-500 text-slate-950 hover:bg-amber-400',
      )}
      type="button"
      variant={active ? 'default' : 'outline'}
      onClick={onClick}
    >
      {label}
    </Button>
  )
}

export function ConfirmationCaseBadge({
  type,
}: {
  type: ConfirmationCaseType
}) {
  const isRejected = type === 'abgelehnt'

  return (
    <span
      className={cn(
        'inline-flex items-center gap-2 rounded-full border px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em]',
        isRejected
          ? 'border-red-200 bg-red-100 text-red-700'
          : 'border-amber-200 bg-amber-100 text-amber-700',
      )}
    >
      {isRejected ? (
        <XCircle className="size-3.5" />
      ) : (
        <AlertCircle className="size-3.5" />
      )}
      {confirmationCaseLabel(type)}
    </span>
  )
}

export function ChannelButton({
  active,
  icon,
  label,
  onClick,
}: {
  active: boolean
  icon: ReactNode
  label: string
  onClick: () => void
}) {
  return (
    <button
      className={cn(
        'flex h-12 items-center justify-center gap-2 rounded-2xl border text-sm font-medium transition',
        active
          ? 'border-slate-900 bg-slate-900 text-white'
          : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50',
      )}
      type="button"
      onClick={onClick}
    >
      {icon}
      {label}
    </button>
  )
}

export function DetailGridItem({
  icon,
  label,
  value,
  valueHint,
  valueClassName,
}: {
  icon?: ReactNode
  label: string
  value: string
  valueHint?: string
  valueClassName?: string
}) {
  return (
    <div className="grid gap-1">
      <p className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-[0.18em] text-muted-foreground">
        {icon}
        {label}
      </p>
      <p className={cn('text-sm text-foreground', valueClassName)}>{value}</p>
      {valueHint ? (
        <p className="text-xs text-muted-foreground">{valueHint}</p>
      ) : null}
    </div>
  )
}

export { channelLabel }
