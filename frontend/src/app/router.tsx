import { ConfirmationPage } from '../pages/confirmation/page'
import { DashboardPage } from '../pages/dashboard/page'

function normalizePath(pathname: string) {
  return pathname.replace(/\/+$/, '') || '/'
}

export function App() {
  const pathname = normalizePath(window.location.pathname)

  if (pathname.startsWith('/dashboard')) {
    return <DashboardPage />
  }

  return <ConfirmationPage />
}
