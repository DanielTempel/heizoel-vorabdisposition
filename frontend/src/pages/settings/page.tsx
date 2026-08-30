import { useEffect, useState } from 'react'
import { ArrowLeft, CheckCircle2, CircleAlert } from 'lucide-react'
import { Link } from 'react-router-dom'
import { ApiError } from '@/api/dashboard-api'
import {
  getEmailSettings,
  type EmailSettings,
  type SmtpSecurityMode,
} from '@/api/settings-api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

type PageStatus = 'loading' | 'ready' | 'error'

const securityModeLabels: Record<SmtpSecurityMode, string> = {
  STARTTLS: 'STARTTLS',
  IMPLICIT_TLS: 'SSL/TLS',
  NONE: 'Keine Verschlüsselung',
}

function SettingsField({
  label,
  value,
}: {
  label: string
  value: string
}) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="mt-0.5 text-sm font-medium">{value}</dd>
    </div>
  )
}

function formatUpdatedAt(value: string | null) {
  if (value === null) {
    return '–'
  }

  return new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

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
        <p className="text-sm text-muted-foreground">Administration</p>
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
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <CardTitle>E-Mail-Versand</CardTitle>
                <CardDescription className="mt-1">
                  SMTP-Konfiguration für Avisierungsanfragen
                </CardDescription>
              </div>
              <div className="flex items-center gap-1.5 text-xs font-medium">
                {settings.configured ? (
                  <>
                    <CheckCircle2 className="size-4 text-green-700" />
                    Konfiguriert
                  </>
                ) : (
                  <>
                    <CircleAlert className="size-4 text-amber-700" />
                    Nicht konfiguriert
                  </>
                )}
              </div>
            </div>
          </CardHeader>
          <CardContent>
            {settings.configured ? (
              <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                <SettingsField
                  label="SMTP-Server"
                  value={settings.smtpHost ?? '–'}
                />
                <SettingsField
                  label="Port"
                  value={settings.smtpPort?.toString() ?? '–'}
                />
                <SettingsField
                  label="Verschlüsselung"
                  value={
                    settings.securityMode === null
                      ? '–'
                      : securityModeLabels[settings.securityMode]
                  }
                />
                <SettingsField
                  label="Authentifizierung"
                  value={settings.authenticationEnabled ? 'Aktiv' : 'Inaktiv'}
                />
                <SettingsField
                  label="Benutzername"
                  value={settings.username ?? '–'}
                />
                <SettingsField
                  label="Passwort"
                  value={settings.passwordConfigured ? 'Hinterlegt' : 'Nicht hinterlegt'}
                />
                <SettingsField
                  label="Absenderadresse"
                  value={settings.fromAddress ?? '–'}
                />
                <SettingsField
                  label="Absendername"
                  value={settings.fromName ?? '–'}
                />
                <SettingsField
                  label="Zuletzt geändert"
                  value={formatUpdatedAt(settings.updatedAt)}
                />
              </dl>
            ) : (
              <p className="text-sm text-muted-foreground">
                Für dieses Unternehmen sind noch keine SMTP-Einstellungen
                hinterlegt.
              </p>
            )}
          </CardContent>
        </Card>
      ) : null}
    </section>
  )
}
