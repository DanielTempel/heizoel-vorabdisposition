import path from 'node:path'
import { fileURLToPath } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { dispoDemo } from './dev/dispo-demo'

const dirname = path.dirname(fileURLToPath(import.meta.url))

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, dirname, ['VITE_', 'DISPO_BACKEND_URL'])
  const backendEnv = loadEnv(mode, path.resolve(dirname, '../backend'), 'DEV_API_KEY')
  return {
    plugins: [react(), tailwindcss(), dispoDemo(
      backendEnv.DEV_API_KEY,
      env.DISPO_BACKEND_URL || env.VITE_API_BASE_URL || 'http://localhost:8080',
    )],
    resolve: {
      alias: {
        '@': path.resolve(dirname, './src'),
      },
    },
    server: {
      port: 3000,
      strictPort: true,
    },
  }
})
