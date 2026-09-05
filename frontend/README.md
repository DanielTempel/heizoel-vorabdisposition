# Frontend

React frontend for customer delivery confirmations and the dispatcher dashboard.

## Run everything with Docker Compose

Create `backend/.env` with the backend settings described in the
[backend README](../backend/README.md). Set `DEV_API_KEY` there to the existing
API key of the company whose dashboard data you want to present.

From the `backend` directory:

```sh
docker compose up -d --build
```

Open <http://localhost:3000/dispo> and click **Avisierungsdashboard öffnen** once
the backend has finished starting. The button requests a fresh access link and
opens the existing login flow. Existing company data is used; the page does not
create demonstration orders or change the company's API key.

Compose runs Vite in a Node.js container. Node.js and a separate `npm run dev`
are not needed on the host. Stop any local frontend already using port 3000.
After source changes, rebuild with `docker compose up -d --build frontend`.
After changing `DEV_API_KEY` in `backend/.env`, run `docker compose up -d frontend`
to recreate the container with the new value. To stop the stack, run
`docker compose stop` from `backend`.

The browser calls `http://localhost:8080`. The Vite demo handler calls the same
backend through Docker's internal address `http://backend:8080`, configured by
`DISPO_BACKEND_URL`. Compose passes only `DEV_API_KEY` to the frontend for demo
access; the key is not copied into the image or exposed in browser code.
The frontend port is published on the local computer only. Use `localhost`
consistently with the backend's configured frontend/CORS URL.

## Run the frontend separately

Start the backend services, leaving the frontend container stopped:

```sh
# From backend/
docker compose up -d --build backend postgres mailpit pgadmin dispo-mock
```

If the frontend container is already running, first run
`docker compose stop frontend` from `backend` to free port 3000.
Then, from `frontend`, with Node.js 24 installed:

```sh
npm ci
npm run dev
```

Vite reads `DEV_API_KEY` from `backend/.env`; no duplicate frontend key is needed.
Restart Vite after changing that file. If the backend address differs from
`http://localhost:8080`, copy `.env.example` to `.env.local` and set
`VITE_API_BASE_URL` to the address reachable from the browser. If Vite needs a
different address for the same backend, also set `DISPO_BACKEND_URL`.
With default addresses, no frontend environment file is needed.

## Demo scope and checks

The Dispo screenshot is illustrative; only the dashboard button is interactive.
The demo page and server handler run under `npm run dev`, including in Docker.
They are not available in the static output from `npm run build` or in
`npm run preview`. This Compose setup is intended for local development and
presentations.

Verification: `npm run build` and `npm run lint`.
