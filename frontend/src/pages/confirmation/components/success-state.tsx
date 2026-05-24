import { formatDate, formatTime } from '../../../lib/format-delivery'
import type {
  CustomerAnswerType,
  CustomerConfirmationPreview,
} from '../../../types/confirmation'

type SuccessStateProps = {
  answerType: CustomerAnswerType | null
  confirmation: CustomerConfirmationPreview
}

export function SuccessState({
  answerType,
  confirmation,
}: SuccessStateProps) {
  const statusText =
    answerType === 'reject'
      ? 'Der Liefertermin wurde abgelehnt.'
      : 'Der Liefertermin wurde bestaetigt.'

  return (
    <main className="page-shell">
      <section className="content-card">
        <p className="eyebrow">Rückmeldung erhalten</p>
        <h1>Vielen Dank für Ihre Rückmeldung</h1>
        <p>Ihre Antwort wurde erfolgreich übermittelt.</p>
        <p>{statusText}</p>

        <div className="details-list">
          <p>
            <strong>Lieferdatum:</strong>{' '}
            {formatDate(confirmation.deliveryDate)} -{' '}
            {formatTime(confirmation.deliveryWindowStart)} -{' '}
            {formatTime(confirmation.deliveryWindowEnd)} Uhr
          </p>
          <p>
            <strong>Lieferadresse:</strong> {confirmation.deliveryAddress}
          </p>
          <p>
            <strong>Produkt / Menge:</strong> {confirmation.product} -{' '}
            {confirmation.quantityLiters.toLocaleString('de-DE')} Liter
          </p>
        </div>

        <p className="close-note">
          Sie können dieses Fenster nun schliessen.
        </p>
      </section>
    </main>
  )
}
