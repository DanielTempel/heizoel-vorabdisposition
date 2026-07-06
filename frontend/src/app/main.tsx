import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './router'
import './globals.css'

async function enableMocking() {
  if (import.meta.env.VITE_CONFIRMATION_API_MODE !== 'mock') {
    return
  }

  const { worker } = await import('../mocks/browser')

  return worker.start({
    onUnhandledRequest: 'bypass',
  })
}

function renderApp() {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
}

enableMocking().then(renderApp)
