import { Card, CardContent } from '@/components/ui/card'

export function LoadingState() {
  return (
    <main className="min-h-screen bg-background px-6 py-16 text-foreground">
      <Card className="mx-auto w-full max-w-6xl rounded-3xl p-8">
        <CardContent className="px-0 text-center text-sm text-muted-foreground">
          Lieferdaten werden geladen...
        </CardContent>
      </Card>
    </main>
  )
}
