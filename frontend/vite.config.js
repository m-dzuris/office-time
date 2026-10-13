import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    // Spring Boot serves classpath:/static, so build straight into the backend's output folder.
    outDir: '../target/classes/static',
    emptyOutDir: true,
  },
  server: {
    // `npm run dev` talks to a backend started separately on :8080.
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'node',
  },
})
