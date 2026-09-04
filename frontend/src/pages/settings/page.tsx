import { useEffect, useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { Link } from 'react-router-dom'
import { ApiError } from '@/api/dashboard-api'
import { getEmailSettings, type EmailSettings } from '@/api/settings-api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { EmailSettingsForm } from './components/email-settings-form'

type PageStatus = 'loading' | 'ready' | 'error'

function getErrorMessage(error: unknown) {
  if (error instanceof ApiError && error.status === 401) {
    return 'Die Dashboard-Sitzung ist abgelaufen oder nicht mehr gültig.'
  }

  return 'Die E-Mail-Einstellungen konnten nicht geladen werden.'
}

export function SettingsPage() {
  const [reloadKey, setReloadKey] = useState(0)
  const [status, setStatus] = useState<PageStatus>('loading')
  const [settings, setSettings] = useState<EmailSettings | null>(null)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadSettings() {
      try {
        const loadedSettings = await getEmailSettings(controller.signal)

        if (!controller.signal.aborted) {
          setSettings(loadedSettings)
          setStatus('ready')
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          setErrorMessage(getErrorMessage(error))
          setStatus('error')
        }
      }
    }

    void loadSettings()

    return () => controller.abort()
  }, [reloadKey])

  function reload() {
    setStatus('loading')
    setReloadKey((currentKey) => currentKey + 1)
  }

  return (
    <section className="grid gap-6" aria-labelledby="settings-title">
      <div>
        <Button asChild variant="outline">
          <Link to="/dashboard">
            <ArrowLeft />
            Zurück zur Tourübersicht
          </Link>
        </Button>
      </div>

      <header>
        <h1 className="mt-1 text-2xl font-semibold" id="settings-title">
          Einstellungen
        </h1>
      </header>

      {status === 'loading' ? (
        <div
          aria-live="polite"
          className="rounded-lg border border-dashed bg-background p-10 text-center text-sm text-muted-foreground"
        >
          E-Mail-Einstellungen werden geladen…
        </div>
      ) : null}

      {status === 'error' ? (
        <Alert variant="destructive">
          <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
            <span>{errorMessage}</span>
            <Button onClick={reload} size="sm" variant="outline">
              Erneut versuchen
            </Button>
          </AlertDescription>
        </Alert>
      ) : null}

      {status === 'ready' && settings ? (
        <Card className="max-w-4xl">
          <CardHeader>
            <CardTitle>E-Mail-Versand</CardTitle>
            <CardDescription>
              SMTP-Konfiguration für Avisierungsanfragen
            </CardDescription>
          </CardHeader>
          <CardContent>
            <EmailSettingsForm
              onSaved={setSettings}
              settings={settings}
            />
          </CardContent>
        </Card>
      ) : null}
    </section>
  )
}
