/// <reference types="vitest" />
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

// Each backend service owns a slice of /api; there is no gateway in local development.
const MATCH_SERVICE = process.env.MATCH_SERVICE_URL ?? 'http://localhost:8082'
const SCORING_SERVICE = process.env.SCORING_SERVICE_URL ?? 'http://localhost:8083'
const COMMENTARY_SERVICE = process.env.COMMENTARY_SERVICE_URL ?? 'http://localhost:8084'

export default defineConfig({
  plugins: [react()],
  define: { global: "globalThis" },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api/v1/matches': { target: MATCH_SERVICE, changeOrigin: true },
      '/api/v1/score': { target: SCORING_SERVICE, changeOrigin: true },
      '/api/v1/commentary': { target: COMMENTARY_SERVICE, changeOrigin: true },
      '/ws': { target: COMMENTARY_SERVICE, ws: true, changeOrigin: true },
    },
  },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
