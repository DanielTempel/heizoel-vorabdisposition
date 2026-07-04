import { useEffect, useState } from 'react'
import { ConfirmationPage } from '../pages/confirmation/page'
import { DashboardPage } from '../pages/dashboard/page'
import { NewTimeWindowPage } from '../pages/confirmation-cases/components/new-time-window-page'

function normalizePath(pathname: string) {
  return pathname.replace(/\/+$/, '') || '/'
}

function extractOrderId(pathname: string) {
  const match = pathname.match(/^\/dashboard\/confirmation\/([^/]+)$/)
  return match ? decodeURIComponent(match[1]) : null
}

export function App() {
  const [pathname, setPathname] = useState(() =>
    normalizePath(window.location.pathname),
  )

  useEffect(() => {
    function handleLocationChange() {
      setPathname(normalizePath(window.location.pathname))
    }

    window.addEventListener('popstate', handleLocationChange)
    window.addEventListener('locationchange', handleLocationChange)

    return () => {
      window.removeEventListener('popstate', handleLocationChange)
      window.removeEventListener('locationchange', handleLocationChange)
    }
  }, [])

  function navigate(path: string) {
    const nextPath = normalizePath(path)

    if (nextPath === pathname) {
      return
    }

    window.history.pushState({}, '', nextPath)
    window.dispatchEvent(new Event('locationchange'))
  }

  const orderId = extractOrderId(pathname)

  if (orderId !== null) {
    return <NewTimeWindowPage navigate={navigate} orderId={orderId} />
  }

  if (pathname.startsWith('/dashboard')) {
    return <DashboardPage />
  }

  return <ConfirmationPage />
}
