import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Keeps the dev server same-origin with the backend (localhost:8080),
      // matching the nginx reverse-proxy setup used in docker-compose, so
      // the httpOnly refresh cookie and SameSite=Strict behave identically
      // in local dev and in the container.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
