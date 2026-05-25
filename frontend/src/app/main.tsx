import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ConfirmationPage } from '../pages/confirmation/page'
import './globals.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfirmationPage />
  </StrictMode>,
)
