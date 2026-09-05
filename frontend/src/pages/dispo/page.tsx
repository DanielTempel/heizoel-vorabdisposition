import { useState } from 'react'
import { ArrowUpRight, LayoutDashboard, LoaderCircle, Truck } from 'lucide-react'
import screenshot from './dispo-screenshot.jpg'

export function DispoPage() {
  const [isOpening, setIsOpening] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function openDashboard() {
    if (isOpening) return
    setIsOpening(true)
    setError(null)
    try {
      const response = await fetch('/api/demo/dashboard-access', {
        method: 'POST',
        headers: { 'X-Dispo-Demo': '1' },
      })
      const result = await response.json() as { url?: string; message?: string }
      if (!response.ok || !result.url?.startsWith('/login?code=')) {
        throw new Error(result.message || 'Das Dashboard konnte nicht geöffnet werden.')
      }
      window.location.assign(result.url)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Bitte erneut versuchen.')
      setIsOpening(false)
    }
  }

  return (
    <main className="flex min-h-screen flex-col bg-slate-100 text-slate-900 [color-scheme:light]">
      <header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 bg-white px-6 py-4 lg:px-10">
        <div className="flex items-center gap-3">
          <div className="rounded-lg bg-cyan-700 p-2.5 text-white"><Truck className="size-6" aria-hidden="true" /></div>
          <div>
            <p className="text-lg font-semibold tracking-tight">Dispo</p>
            <p className="text-xs text-slate-500">Disposition & Tourenplanung</p>
          </div>
        </div>
        <span className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-500">Demonstration</span>
      </header>

      <section className="flex flex-1 flex-col gap-6 px-4 py-8 sm:px-6 lg:px-10">
        <div className="flex flex-wrap items-end justify-between gap-5">
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-cyan-700">Arbeitsplatz Disposition</p>
            <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Alles für die nächste Tour.</h1>
            <p className="mt-2 text-sm text-slate-500">Touren planen. Lieferungen abstimmen. Bestätigungen im Blick behalten.</p>
          </div>
          <button
            type="button"
            onClick={openDashboard}
            disabled={isOpening}
            className="flex min-h-12 items-center justify-center gap-3 rounded-lg bg-cyan-700 px-5 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-cyan-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-cyan-700 disabled:cursor-wait disabled:opacity-70"
          >
            {isOpening ? <LoaderCircle className="size-5 animate-spin" aria-hidden="true" /> : <LayoutDashboard className="size-5" aria-hidden="true" />}
            {isOpening ? 'Dashboard wird geöffnet …' : 'Avisierungsdashboard öffnen'}
            {!isOpening && <ArrowUpRight className="size-4" aria-hidden="true" />}
          </button>
        </div>

        {error && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">{error}</p>}

        <figure className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <figcaption className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 px-4 py-3 text-xs text-slate-500">
            <span className="font-medium text-slate-700">Dispo · Tourenübersicht</span>
            <span>Ansicht des Dispo-Systems</span>
          </figcaption>
          <div className="overflow-x-auto">
            <img src={screenshot} alt="Dispo-Oberfläche mit Karte, offenen Lieferungen und Tourenplanung" className="block h-auto w-full min-w-250" width="2560" height="777" />
          </div>
        </figure>

        <p className="text-xs text-slate-500">Die Dispo-Ansicht dient der Veranschaulichung. Über den Button öffnen Sie das Avisierungsdashboard.</p>
      </section>
      <footer className="border-t border-slate-200 px-6 py-4 text-xs text-slate-500 lg:px-10">Heizöl-Vorabdisposition · Projektpräsentation</footer>
    </main>
  )
}
