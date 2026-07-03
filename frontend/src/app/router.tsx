import { ConfirmationPage } from '../pages/confirmation/page'
import { DashboardPage } from '../pages/dashboard/page'

export function App() {
  if (window.location.pathname.startsWith('/dashboard')) {
    return <DashboardPage />
  }

  return <ConfirmationPage />
}
