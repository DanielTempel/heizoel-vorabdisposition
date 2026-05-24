import { useEffect, useState } from 'react'
import {
  confirmDelivery,
  getConfirmationPreview,
  rejectDelivery,
} from '../../api/confirmation-api'
import { formatDate, formatTime } from '../../lib/format-delivery'
import type {
  CustomerAnswerType,
  CustomerConfirmationPreview,
} from '../../types/confirmation'
import { ErrorState } from './components/error-state'
import { LoadingState } from './components/loading-state'
import { SuccessState } from './components/success-state'

type PageStatus = 'loading' | 'ready' | 'submitting' | 'success' | 'error'

function getTokenFromPath() {
  const pathParts = window.location.pathname.split('/').filter(Boolean)
  const confirmationIndex = pathParts.indexOf('confirmation')

  if (confirmationIndex === -1) {
    return 'mock-token'
  }

  return pathParts[confirmationIndex + 1] ?? 'mock-token'
}

export function ConfirmationPage() {
  const [status, setStatus] = useState<PageStatus>('loading')
  const [confirmation, setConfirmation] =
    useState<CustomerConfirmationPreview | null>(null)
  const [comment, setComment] = useState('')
  const [answerType, setAnswerType] = useState<CustomerAnswerType | null>(null)

  const token = getTokenFromPath()

  useEffect(() => {
    async function loadConfirmation() {
      try {
        const preview = await getConfirmationPreview(token)

        setConfirmation(preview)
        setStatus('ready')
      } catch {
        setStatus('error')
      }
    }

    void loadConfirmation()
  }, [token])

  async function submitAnswer(type: CustomerAnswerType) {
    setStatus('submitting')
    setAnswerType(type)

    try {
      const request = comment.trim()
        ? { customerComment: comment.trim() }
        : {}

      if (type === 'confirm') {
        await confirmDelivery(token, request)
      } else {
        await rejectDelivery(token, request)
      }

      setStatus('success')
    } catch {
      setStatus('error')
    }
  }

  if (status === 'loading') {
    return <LoadingState />
  }

  if (status === 'error' || confirmation === null) {
    return <ErrorState />
  }

  if (status === 'success') {
    return (
      <SuccessState answerType={answerType} confirmation={confirmation} />
    )
  }

  const isSubmitting = status === 'submitting'

  return (
    <main className="page-shell">
      <section className="content-card">
        <h1>Bestätigen Sie Ihren Liefertermin</h1>
        <p>
          Bitte prüfen Sie die geplanten Lieferdaten. Wenn der Termin passt,
          bestätigen Sie die Lieferung. Falls der Termin nicht passt, können
          Sie ihn ablehnen und eine kurze Nachricht hinterlassen.
        </p>

        <section className="date-panel">
          <p className="eyebrow">Lieferdatum</p>
          <h2>{formatDate(confirmation.deliveryDate)}</h2>
          <p>
            {formatTime(confirmation.deliveryWindowStart)} -{' '}
            {formatTime(confirmation.deliveryWindowEnd)} Uhr
          </p>
        </section>

        <section>
          <p className="eyebrow">Lieferdetails</p>
          <div className="details-list">
            <p>
              <strong>Auftragsnummer:</strong> {confirmation.externalOrderId}
            </p>
            <p>
              <strong>Kunde:</strong> {confirmation.customerName}
            </p>
            <p>
              <strong>Lieferadresse:</strong>{' '}
              {confirmation.deliveryAddress}
            </p>
            <p>
              <strong>Produkt:</strong> {confirmation.product}
            </p>
            <p>
              <strong>Menge:</strong>{' '}
              {confirmation.quantityLiters.toLocaleString('de-DE')} Liter
            </p>
          </div>
        </section>

        <label className="comment-field">
          Nachricht an die Disposition (optional)
          <textarea
            value={comment}
            maxLength={2000}
            placeholder="Nachricht hier schreiben..."
            onChange={(event) => setComment(event.target.value)}
          />
        </label>

        <p className="notice">
          Bitte beachten Sie: Diese Anfrage kann nur einmal beantwortet werden.
        </p>

        <div className="actions">
          <button
            className="primary-action"
            disabled={isSubmitting}
            type="button"
            onClick={() => void submitAnswer('confirm')}
          >
            Termin bestätigen
          </button>
          <button
            className="danger-action"
            disabled={isSubmitting}
            type="button"
            onClick={() => void submitAnswer('reject')}
          >
            Termin ablehnen
          </button>
        </div>
      </section>
    </main>
  )
}
