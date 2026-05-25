import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export function ErrorState() {
  return (
    <main className="min-h-screen bg-background px-6 py-16 text-foreground">
      <Card className="mx-auto w-full max-w-6xl rounded-3xl p-4 sm:p-8">
        <CardHeader className="items-center px-0 text-center">
          <p className="text-xs font-semibold uppercase text-destructive">
            Fehler - Link ungültig
          </p>
          <CardTitle className="max-w-md text-2xl font-semibold">
            Dieser Link ist nicht mehr gültig
          </CardTitle>
        </CardHeader>

        <CardContent className="grid gap-6 px-0">
          <p className="text-center text-sm text-muted-foreground">
            Die Anfrage wurde bereits beantwortet oder ist abgelaufen. Bitte
            wenden Sie sich bei Fragen an Ihre Disposition.
          </p>

          <Alert variant="destructive" className="p-4">
            <AlertTitle>Mögliche Gründe</AlertTitle>
            <AlertDescription>
              Der Bestätigungslink wurde bereits benutzt, der Termin wurde
              systemseitig bearbeitet oder der Link ist abgelaufen.
            </AlertDescription>
          </Alert>

          <p className="text-center font-semibold">
            Sie können dieses Fenster nun schließen.
          </p>
        </CardContent>
      </Card>
    </main>
  )
}
