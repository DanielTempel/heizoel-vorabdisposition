import { useEffect, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import { getTours } from '@/api/dashboard-api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import type { ToursPage } from '@/types/dashboard'
import { TourItem } from './components/tour-item'

type PageStatus = 'loading' | 'ready' | 'error'

export function DashboardPage() {
  const [page, setPage] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [status, setStatus] = useState<PageStatus>('loading')
  const [toursPage, setToursPage] = useState<ToursPage | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    async function loadTours() {
      try {
        const nextToursPage = await getTours(page, controller.signal)

        if (!controller.signal.aborted) {
          setToursPage(nextToursPage)
          setStatus('ready')
        }
      } catch {
        if (!controller.signal.aborted) {
          setStatus('error')
        }
      }
    }

    void loadTours()

    return () => controller.abort()
  }, [page, reloadKey])

  function changePage(nextPage: number) {
    setStatus('loading')
    setPage(nextPage)
  }

  function reload() {
    setStatus('loading')
    setReloadKey((currentKey) => currentKey + 1)
  }

  const totalPages = toursPage?.totalPages ?? 0

  return (
    <main className="min-h-screen bg-muted/20 px-4 py-8 text-foreground sm:px-6">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-6">
        <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
              Disposition
            </p>
            <h1 className="mt-1 text-3xl font-semibold">
              Avisierungsdashboard
            </h1>
            <p className="mt-2 max-w-3xl text-sm text-muted-foreground">
              Touren und Rückmeldungen zu den geplanten Lieferzeitfenstern.
            </p>
          </div>
          <Button
            disabled={status === 'loading'}
            onClick={reload}
            variant="outline"
          >
            <RefreshCw className={status === 'loading' ? 'animate-spin' : ''} />
            Aktualisieren
          </Button>
        </header>

        {status === 'loading' ? (
          <div className="rounded-lg border border-dashed bg-background p-10 text-center text-sm text-muted-foreground">
            Touren werden geladen…
          </div>
        ) : null}

        {status === 'error' ? (
          <Alert className="border-red-300 bg-red-50">
            <AlertDescription className="flex flex-wrap items-center justify-between gap-3 text-red-950">
              <span>Die Touren konnten nicht geladen werden.</span>
              <Button onClick={reload} size="sm" variant="outline">
                Erneut versuchen
              </Button>
            </AlertDescription>
          </Alert>
        ) : null}

        {status === 'ready' && toursPage?.items.length === 0 ? (
          <div className="rounded-lg border bg-background p-10 text-center text-sm text-muted-foreground">
            Keine Touren vorhanden.
          </div>
        ) : null}

        {status === 'ready' && toursPage && toursPage.items.length > 0 ? (
          <section aria-label="Touren" className="grid gap-3">
            {toursPage.items.map((tour) => (
              <TourItem
                key={`${tour.tourNumber}-${tour.deliveryDate}`}
                tour={tour}
              />
            ))}
          </section>
        ) : null}

        {status === 'ready' && toursPage && totalPages > 0 ? (
          <footer className="flex flex-col gap-3 border-t pt-4 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
            <span>{toursPage.totalElements} Touren insgesamt</span>
            <div className="flex items-center gap-2">
              <Button
                disabled={page === 0}
                onClick={() => changePage(page - 1)}
                variant="outline"
              >
                Zurück
              </Button>
              <span className="min-w-24 text-center">
                Seite {page + 1} von {totalPages}
              </span>
              <Button
                disabled={page + 1 >= totalPages}
                onClick={() => changePage(page + 1)}
                variant="outline"
              >
                Weiter
              </Button>
            </div>
          </footer>
        ) : null}
      </div>
    </main>
  )
}
