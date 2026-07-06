import { useEffect, useState } from 'react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { getDashboardConfirmations } from '../../api/dashboard-api'
import type { DashboardConfirmation } from '../../types/dashboard'
import { DashboardTable } from './components/dashboard-table'

type PageStatus = 'loading' | 'ready' | 'error'

export function DashboardPage() {
  const [status, setStatus] = useState<PageStatus>('loading')
  const [confirmations, setConfirmations] = useState<DashboardConfirmation[]>(
    [],
  )

  useEffect(() => {
    async function loadDashboard() {
      try {
        const nextConfirmations = await getDashboardConfirmations()

        setConfirmations(nextConfirmations)
        setStatus('ready')
      } catch {
        setStatus('error')
      }
    }

    void loadDashboard()
  }, [])

  function openConfirmationDetails(orderId: string) {
    window.history.pushState({}, '', `/dashboard/confirmation/${orderId}`)
    window.dispatchEvent(new Event('locationchange'))
  }

  return (
    <main className="min-h-screen bg-background px-6 py-8 text-foreground">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-6">
        <header>
          <h1 className="mt-2 text-3xl font-semibold">
            Dashboard
          </h1>
          <p className="mt-2 max-w-3xl text-sm text-muted-foreground">
            Übersicht der digitalen Rückmeldungen zu geplanten
            Heizöl-Lieferterminen.
          </p>
        </header>

        <section className="grid gap-4">
          <div className="overflow-hidden rounded-lg border bg-background">
            {status === 'loading' ? (
              <div className="rounded-lg border border-dashed p-8 text-center text-muted-foreground">
                Dashboard wird geladen...
              </div>
            ) : null}

            {status === 'error' ? (
              <Alert className="border-red-300 bg-red-50">
                <AlertDescription className="text-red-950">
                  Dashboard-Daten konnten nicht geladen werden.
                </AlertDescription>
              </Alert>
            ) : null}

            {status === 'ready' && confirmations.length === 0 ? (
              <div className="p-8 text-center text-muted-foreground">
                Keine Rückmeldeanfragen vorhanden.
              </div>
            ) : null}

            {status === 'ready' && confirmations.length > 0 ? (
              <DashboardTable
                confirmations={confirmations}
                onViewDetails={openConfirmationDetails}
              />
            ) : null}
          </div>
        </section>
      </div>
    </main>
  )
}
