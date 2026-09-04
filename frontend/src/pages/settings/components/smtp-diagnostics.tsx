import { useState } from 'react'
import { CheckCircle2, LoaderCircle, MailCheck, PlugZap } from 'lucide-react'
import { ApiError } from '@/api/dashboard-api'
import { sendTestEmail, testEmailConnection } from '@/api/settings-api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'

type DiagnosticAction = 'connection' | 'message'

type DiagnosticState =
  | { status: 'idle' }
  | { status: 'loading'; action: DiagnosticAction }
  | { status: 'success'; message: string }
  | { status: 'error'; message: string }

type SmtpDiagnosticsProps = {
  configured: boolean
  hasUnsavedChanges: boolean
  isSaving: boolean
  recipient: string | null
}

function getErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) {
    return 'Die SMTP-Prüfung konnte nicht durchgeführt werden.'
  }

  if (error.status === 401) {
    return 'Die Dashboard-Sitzung ist abgelaufen oder nicht mehr gültig.'
  }

  if (error.code === 'EMAIL_SETTINGS_NOT_CONFIGURED') {
    return 'Speichern Sie zuerst eine vollständige SMTP-Konfiguration.'
  }

  if (error.code === 'SMTP_CONNECTION_FAILED') {
    return 'Die Verbindung zum SMTP-Server konnte nicht hergestellt werden.'
  }

  if (error.code === 'SMTP_TEST_MESSAGE_FAILED') {
    return 'Die Test-E-Mail konnte nicht versendet werden.'
  }

  return 'Die SMTP-Prüfung konnte nicht durchgeführt werden.'
}

export function SmtpDiagnostics({
  configured,
  hasUnsavedChanges,
  isSaving,
  recipient,
}: SmtpDiagnosticsProps) {
  const [state, setState] = useState<DiagnosticState>({ status: 'idle' })
  const isLoading = state.status === 'loading'
  const isDisabled =
    !configured || hasUnsavedChanges || isSaving || isLoading

  async function run(action: DiagnosticAction) {
    if (isDisabled) {
      return
    }

    setState({ status: 'loading', action })

    try {
      if (action === 'connection') {
        await testEmailConnection()
        setState({
          status: 'success',
          message: 'Die Verbindung zum SMTP-Server war erfolgreich.',
        })
      } else {
        await sendTestEmail()
        setState({
          status: 'success',
          message: `Der SMTP-Server hat die Test-E-Mail für ${recipient ?? 'die Absenderadresse'} angenommen.`,
        })
      }
    } catch (error) {
      setState({ status: 'error', message: getErrorMessage(error) })
    }
  }

  return (
    <section
      className="grid gap-3 border-t pt-4"
      aria-labelledby="smtp-test-title"
    >
      <div>
        <h3 className="text-sm font-medium" id="smtp-test-title">
          SMTP-Konfiguration testen
        </h3>
        <p className="mt-1 text-xs text-muted-foreground">
          Die Tests verwenden die zuletzt gespeicherten Einstellungen. Die
          Test-E-Mail wird an die Absenderadresse gesendet.
        </p>
      </div>

      {!configured ? (
        <p className="text-xs text-muted-foreground">
          Speichern Sie zuerst die SMTP-Einstellungen.
        </p>
      ) : null}

      {hasUnsavedChanges ? (
        <p className="text-xs text-amber-800">
          Speichern oder verwerfen Sie zuerst die aktuellen Änderungen.
        </p>
      ) : null}

      {state.status === 'success' || state.status === 'error' ? (
        <Alert
          className={
            state.status === 'success'
              ? 'border-green-200 bg-green-50 text-green-700'
              : undefined
          }
          variant={state.status === 'error' ? 'destructive' : 'default'}
        >
          {state.status === 'success' ? (
            <CheckCircle2 className="size-4" />
          ) : null}
          <AlertDescription
            className={state.status === 'success' ? 'text-green-900' : undefined}
          >
            {state.message}
          </AlertDescription>
        </Alert>
      ) : null}

      <div className="flex flex-wrap gap-2">
        <Button
          disabled={isDisabled}
          onClick={() => void run('connection')}
          type="button"
          variant="outline"
        >
          {state.status === 'loading' && state.action === 'connection' ? (
            <LoaderCircle className="animate-spin" />
          ) : (
            <PlugZap />
          )}
          {state.status === 'loading' && state.action === 'connection'
            ? 'Verbindung wird geprüft…'
            : 'Verbindung testen'}
        </Button>
        <Button
          disabled={isDisabled}
          onClick={() => void run('message')}
          type="button"
          variant="outline"
        >
          {state.status === 'loading' && state.action === 'message' ? (
            <LoaderCircle className="animate-spin" />
          ) : (
            <MailCheck />
          )}
          {state.status === 'loading' && state.action === 'message'
            ? 'Test-E-Mail wird gesendet…'
            : 'Test-E-Mail senden'}
        </Button>
      </div>
    </section>
  )
}
