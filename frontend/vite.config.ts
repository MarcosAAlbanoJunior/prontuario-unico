import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

const backend = 'http://127.0.0.1:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': backend,
      '/actuator': backend,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
});
