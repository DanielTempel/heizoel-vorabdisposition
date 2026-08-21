import { useState, type Dispatch, type SetStateAction } from 'react'
import { Outlet } from 'react-router-dom'
import type { DashboardFilters } from '@/types/dashboard'

export type DashboardNavigationState = {
  page: number
  draftFilters: DashboardFilters
  appliedFilters: DashboardFilters
}

export type DashboardOutletContext = {
  navigationState: DashboardNavigationState
  setNavigationState: Dispatch<SetStateAction<DashboardNavigationState>>
}

function createEmptyFilters(): DashboardFilters {
  return {
    search: '',
    statuses: [],
    dateFrom: '',
    dateTo: '',
  }
}

function createInitialNavigationState(): DashboardNavigationState {
  return {
    page: 0,
    draftFilters: createEmptyFilters(),
    appliedFilters: createEmptyFilters(),
  }
}

export function DashboardLayout() {
  const [navigationState, setNavigationState] = useState(
    createInitialNavigationState,
  )

  return (
    <main className="min-h-screen bg-muted/20 px-4 py-8 text-foreground sm:px-6">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-6">
        <header>
          <h1 className="mt-1 text-3xl font-semibold">
            Avisierungsdashboard
          </h1>
        </header>

        <Outlet context={{ navigationState, setNavigationState }} />
      </div>
    </main>
  )
}
