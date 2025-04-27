import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  base: process.env.VITE_BASE_PATH || '/IT342-MyAlkansya',
  plugins: [
    tailwindcss(),
  ],
})