import { useState, type FormEvent, type ReactNode } from 'react'
import { CheckCircle2 } from 'lucide-react'
import { ApiError } from '@/api/dashboard-api'
import {
  getEmailSettings,
  type EmailSettings,
  type SmtpSecurityMode,
  updateEmailSettings,
} from '@/api/settings-api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

type EmailSettingsFormValues = {
  smtpHost: string
  smtpPort: string
  securityMode: SmtpSecurityMode
  authenticationEnabled: boolean
  username: string
  password: string
  fromAddress: string
  fromName: string
}

const securityModeLabels: Record<SmtpSecurityMode, string> = {
  STARTTLS: 'STARTTLS',
  IMPLICIT_TLS: 'SSL/TLS',
  NONE: 'Keine Verschlüsselung',
}

function createFormValues(settings: EmailSettings): EmailSettingsFormValues {
  return {
    smtpHost: settings.smtpHost ?? '',
    smtpPort: settings.smtpPort?.toString() ?? '',
    securityMode: settings.securityMode ?? 'STARTTLS',
    authenticationEnabled: settings.authenticationEnabled,
    username: settings.username ?? '',
    password: '',
    fromAddress: settings.fromAddress ?? '',
    fromName: settings.fromName ?? '',
  }
}

function FormField({
  children,
  description,
  htmlFor,
  label,
}: {
  children: ReactNode
  description?: string
  htmlFor: string
  label: string
}) {
  return (
    <div className="grid gap-1.5">
      <label className="text-xs font-medium" htmlFor={htmlFor}>
        {label}
      </label>
      {children}
      {description ? (
        <p className="text-xs text-muted-foreground">{description}</p>
      ) : null}
    </div>
  )
}

function formatUpdatedAt(value: string) {
  return new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function getSaveErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) {
    return 'Die E-Mail-Einstellungen konnten nicht gespeichert werden.'
  }

  if (error.status === 401) {
    return 'Die Dashboard-Sitzung ist abgelaufen oder nicht mehr gültig.'
  }

  if (error.code === 'VALIDATION_ERROR') {
    return 'Bitte überprüfen Sie die eingegebenen Werte.'
  }

  return 'Die E-Mail-Einstellungen konnten nicht gespeichert werden.'
}

export function EmailSettingsForm({
  onSaved,
  settings,
}: {
  onSaved: (settings: EmailSettings) => void
  settings: EmailSettings
}) {
  const [form, setForm] = useState(() => createFormValues(settings))
  const [isSaving, setIsSaving] = useState(false)
  const [saveError, setSaveError] = useState('')
  const [saveSucceeded, setSaveSucceeded] = useState(false)

  function updateForm<K extends keyof EmailSettingsFormValues>(
    field: K,
    value: EmailSettingsFormValues[K],
  ) {
    setForm((currentForm) => ({
      ...currentForm,
      [field]: value,
    }))
    setSaveError('')
    setSaveSucceeded(false)
  }

  function resetForm() {
    setForm(createFormValues(settings))
    setSaveError('')
    setSaveSucceeded(false)
  }

  async function saveSettings(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (
      form.authenticationEnabled &&
      form.password.trim() === '' &&
      !settings.passwordConfigured
    ) {
      setSaveError(
        'Für die SMTP-Authentifizierung muss ein Passwort eingegeben werden.',
      )
      return
    }

    setIsSaving(true)
    setSaveError('')
    setSaveSucceeded(false)

    try {
      await updateEmailSettings({
        smtpHost: form.smtpHost.trim(),
        smtpPort: Number(form.smtpPort),
        securityMode: form.securityMode,
        authenticationEnabled: form.authenticationEnabled,
        username: form.authenticationEnabled ? form.username.trim() : null,
        password:
          form.authenticationEnabled && form.password.trim() !== ''
            ? form.password
            : null,
        fromAddress: form.fromAddress.trim(),
        fromName: form.fromName.trim(),
      })

      const updatedSettings = await getEmailSettings()
      setForm(createFormValues(updatedSettings))
      setSaveSucceeded(true)
      onSaved(updatedSettings)
    } catch (error) {
      setSaveError(getSaveErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <form className="grid gap-6" onSubmit={saveSettings}>
      {saveSucceeded ? (
        <Alert className="text-green-700">
          <CheckCircle2 className="size-4" />
          <AlertDescription>
            Die E-Mail-Einstellungen wurden gespeichert.
          </AlertDescription>
        </Alert>
      ) : null}

      {saveError ? (
        <Alert variant="destructive">
          <AlertDescription>{saveError}</AlertDescription>
        </Alert>
      ) : null}

      <div className="grid gap-5 sm:grid-cols-2">
        <FormField htmlFor="smtp-host" label="SMTP-Server">
          <Input
            disabled={isSaving}
            id="smtp-host"
            maxLength={255}
            onChange={(event) => updateForm('smtpHost', event.target.value)}
            required
            value={form.smtpHost}
          />
        </FormField>

        <FormField htmlFor="smtp-port" label="Port">
          <Input
            disabled={isSaving}
            id="smtp-port"
            max={65535}
            min={1}
            onChange={(event) => updateForm('smtpPort', event.target.value)}
            required
            type="number"
            value={form.smtpPort}
          />
        </FormField>

        <FormField htmlFor="security-mode" label="Verschlüsselung">
          <select
            className="h-7 w-full rounded-md border border-input bg-input/20 px-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 disabled:opacity-50 md:text-xs"
            disabled={isSaving}
            id="security-mode"
            onChange={(event) =>
              updateForm(
                'securityMode',
                event.target.value as SmtpSecurityMode,
              )
            }
            value={form.securityMode}
          >
            {Object.entries(securityModeLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </FormField>

        <div className="flex items-center gap-2 self-end pb-1">
          <input
            checked={form.authenticationEnabled}
            className="size-4 accent-primary"
            disabled={isSaving}
            id="authentication-enabled"
            onChange={(event) =>
              updateForm('authenticationEnabled', event.target.checked)
            }
            type="checkbox"
          />
          <label
            className="text-xs font-medium"
            htmlFor="authentication-enabled"
          >
            SMTP-Authentifizierung verwenden
          </label>
        </div>

        <FormField htmlFor="smtp-username" label="Benutzername">
          <Input
            disabled={isSaving || !form.authenticationEnabled}
            id="smtp-username"
            maxLength={320}
            onChange={(event) => updateForm('username', event.target.value)}
            required={form.authenticationEnabled}
            value={form.username}
          />
        </FormField>

        <FormField
          description={
            settings.passwordConfigured
              ? 'Leer lassen, um das gespeicherte Passwort beizubehalten.'
              : undefined
          }
          htmlFor="smtp-password"
          label="Passwort"
        >
          <Input
            autoComplete="new-password"
            disabled={isSaving || !form.authenticationEnabled}
            id="smtp-password"
            maxLength={1000}
            onChange={(event) => updateForm('password', event.target.value)}
            placeholder={
              settings.passwordConfigured ? 'Gespeichertes Passwort' : undefined
            }
            required={
              form.authenticationEnabled && !settings.passwordConfigured
            }
            type="password"
            value={form.password}
          />
        </FormField>

        <FormField htmlFor="from-address" label="Absenderadresse">
          <Input
            disabled={isSaving}
            id="from-address"
            maxLength={320}
            onChange={(event) => updateForm('fromAddress', event.target.value)}
            required
            type="email"
            value={form.fromAddress}
          />
        </FormField>

        <FormField htmlFor="from-name" label="Absendername">
          <Input
            disabled={isSaving}
            id="from-name"
            maxLength={200}
            onChange={(event) => updateForm('fromName', event.target.value)}
            required
            value={form.fromName}
          />
        </FormField>
      </div>

      {settings.updatedAt ? (
        <p className="text-xs text-muted-foreground">
          Zuletzt geändert: {formatUpdatedAt(settings.updatedAt)}
        </p>
      ) : null}

      <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
        <Button
          disabled={isSaving}
          onClick={resetForm}
          type="button"
          variant="outline"
        >
          Änderungen verwerfen
        </Button>
        <Button disabled={isSaving} type="submit">
          {isSaving ? 'Wird gespeichert…' : 'Speichern'}
        </Button>
      </div>
    </form>
  )
}
