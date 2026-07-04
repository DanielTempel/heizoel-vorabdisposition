import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import type { ConfirmationCaseHistoryEvent } from '@/types/confirmation-cases'

export function HistoryCard({
  history,
}: {
  history: ConfirmationCaseHistoryEvent[]
}) {
  return (
    <Card className="rounded-3xl border border-slate-200 bg-white shadow-sm">
      <CardHeader className="border-b border-slate-100 px-6 py-5">
        <CardTitle>Ereignishistorie</CardTitle>
      </CardHeader>
      <CardContent className="px-6 py-6">
        <div className="relative pl-8">
          <div className="absolute bottom-0 left-3 top-1 w-px bg-border" />
          <div className="grid gap-5">
            {history.map((event, index) => (
              <div
                key={`${event.dateLabel}-${event.text}-${index}`}
                className="relative grid gap-1"
              >
                <div
                  className={cn(
                    'absolute -left-[25px] top-1 size-3 rounded-full border-2 bg-background',
                    event.type === 'rejected' && 'border-red-500 bg-red-500',
                    event.type === 'warning' && 'border-amber-500 bg-amber-500',
                    event.type === 'current' && 'border-sky-600 bg-sky-600',
                    event.type === 'sent' && 'border-slate-300',
                  )}
                />
                <p
                  className={cn(
                    'text-xs font-mono text-muted-foreground',
                    event.type === 'current' && 'font-semibold text-sky-700',
                  )}
                >
                  {event.dateLabel}
                </p>
                <p
                  className={cn(
                    'text-sm text-foreground',
                    event.type === 'current' && 'font-semibold text-slate-900',
                  )}
                >
                  {event.text}
                </p>
              </div>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
