import { useEffect, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import { useOutletContext } from 'react-router-dom'
import { getTours } from '@/api/dashboard-api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import type { DashboardOutletContext } from '@/layouts/dashboard-layout'
import type { DashboardFilters, ToursPage } from '@/types/dashboard'
import { FilterPanel } from './components/filter-panel'
import { TourItem } from './components/tour-item'
import { TourPagination } from './components/tour-pagination'

type PageStatus = 'loading' | 'ready' | 'error'

export function DashboardPage() {
  const { navigationState, setNavigationState } = useOutletContext<DashboardOutletContext>()
  const { page, draftFilters, appliedFilters } = navigationState
  const [reloadKey, setReloadKey] = useState(0)
  const [status, setStatus] = useState<PageStatus>('loading')
  const [toursPage, setToursPage] = useState<ToursPage | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    async function loadTours() {
      try {
        const nextToursPage = await getTours(
          { ...appliedFilters, page },
          controller.signal,
        )

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
  }, [appliedFilters, page, reloadKey])

  function changePage(nextPage: number) {
    setStatus('loading')
    setNavigationState((currentState) => ({
      ...currentState,
      page: nextPage,
    }))
  }

  function reload() {
    setStatus('loading')
    setReloadKey((currentKey) => currentKey + 1)
  }

  function applyFilters(filters: DashboardFilters) {
    const nextFilters = {
      ...filters,
      search: filters.search.trim(),
    }

    setStatus('loading')
    setNavigationState({
      page: 0,
      draftFilters: nextFilters,
      appliedFilters: nextFilters,
    })
  }

  const totalPages = toursPage?.totalPages ?? 0
  const hasAppliedFilters =
    appliedFilters.search !== '' ||
    appliedFilters.statuses.length > 0 ||
    appliedFilters.dateFrom !== '' ||
    appliedFilters.dateTo !== ''

  return (
    <>
      <header>
        <h1 className="mt-1 text-3xl font-semibold">
          Avisierungsdashboard
        </h1>
      </header>

      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <FilterPanel
          filters={draftFilters}
          onApply={applyFilters}
          onChange={(filters) =>
            setNavigationState((currentState) => ({
              ...currentState,
              draftFilters: filters,
            }))
          }
        />
        <Button
          disabled={status === 'loading'}
          onClick={reload}
          variant="outline"
        >
          <RefreshCw className={status === 'loading' ? 'animate-spin' : ''} />
          Aktualisieren
        </Button>
      </div>

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
          {hasAppliedFilters
            ? 'Keine Touren entsprechen den ausgewählten Filtern.'
            : 'Keine Touren vorhanden.'}
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
        <TourPagination
          onPageChange={changePage}
          page={page}
          totalElements={toursPage.totalElements}
          totalPages={totalPages}
        />
      ) : null}
    </>
  )
}
