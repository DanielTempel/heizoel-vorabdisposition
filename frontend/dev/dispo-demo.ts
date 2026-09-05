import type { Plugin } from 'vite'

// Only installed on the development server. The API key never enters client code.
export function dispoDemo(apiKey: string | undefined, backendUrl: string): Plugin {
  return {
    name: 'dispo-demo',
    apply: 'serve',
    configureServer(server) {
      server.middlewares.use('/api/demo/dashboard-access', async (req, res) => {
        const reply = (status: number, body: object) => {
          res.writeHead(status, {
            'Content-Type': 'application/json',
            'Cache-Control': 'no-store',
          })
          res.end(JSON.stringify(body))
        }

        if (req.method !== 'POST') {
          res.setHeader('Allow', 'POST')
          reply(405, { message: 'Methode nicht erlaubt.' })
          return
        }

        // Only a same-origin browser action on the local demo can mint a link.
        const host = req.headers.host
        if (
          !host ||
          !/^(localhost|127\.0\.0\.1|\[::1\])(:\d+)?$/.test(host) ||
          req.headers.origin !== `http://${host}` ||
          req.headers['x-dispo-demo'] !== '1'
        ) {
          reply(403, { message: 'Bitte die Demo auf localhost öffnen.' })
          return
        }

        if (!apiKey) {
          reply(503, { message: 'Der Demo-Zugang ist noch nicht eingerichtet.' })
          return
        }

        try {
          const response = await fetch(new URL('/api/dispo/dashboard-access', backendUrl), {
            method: 'POST',
            headers: { 'X-API-Key': apiKey },
            signal: AbortSignal.timeout(10_000),
            redirect: 'error',
          })

          if (!response.ok) {
            reply(502, {
              message: response.status === 401 || response.status === 403
                ? 'Der Demo-Zugang wurde abgelehnt. Bitte die Konfiguration prüfen.'
                : 'Das Dashboard ist gerade nicht erreichbar. Bitte erneut versuchen.',
            })
            return
          }

          const url = new URL((await response.text()).trim())
          if (url.pathname !== '/login' || !url.searchParams.get('code')) {
            throw new Error('Invalid dashboard access response')
          }
          // Stay on this frontend origin and reuse its existing login/session flow.
          reply(200, { url: `/login?code=${encodeURIComponent(url.searchParams.get('code')!)}` })
        } catch {
          reply(502, { message: 'Keine Verbindung zum Backend. Bitte den Backend-Start prüfen und erneut versuchen.' })
        }
      })
    },
  }
}
