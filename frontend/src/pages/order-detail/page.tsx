import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'

export function OrderDetailPage() {
  const { externalOrderId } = useParams()

  return (
    <section className="grid gap-4" aria-labelledby="order-detail-title">
      <div>
        <Button asChild variant="outline">
          <Link to="/dashboard">
            <ArrowLeft />
            Zurück zur Tourübersicht
          </Link>
        </Button>
      </div>

      <div>
        <p className="text-sm text-muted-foreground">Auftragsdetails</p>
        <h2 className="text-2xl font-semibold" id="order-detail-title">
          Auftrag {externalOrderId}
        </h2>
      </div>

      <div className="rounded-lg border border-dashed bg-background p-10 text-center text-sm text-muted-foreground">
        Die Auftragsdaten werden im nächsten Entwicklungsschritt angebunden.
      </div>
    </section>
  )
}
