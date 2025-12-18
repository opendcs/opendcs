import { resolve } from 'path';
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  optimizeDeps: {
    exclude: ['opendcs-api'] // Replace with actual package name
  },
  server: {
    fs: {
      // Allow serving files from one level up the project root
      allow: [".", resolve(__dirname, "../../java/api-clients/api-client-typescript/build/generated/openApi")],
      // Or explicitly allow the path to your linked package
      // allow: [path.resolve(__dirname, '../path/to/your/linked-package')]
    },
    proxy: {
      // Proxy requests starting with '/api'
      '/odcsapi': {
        target: 'http://localhost:7000', // The address of your backend server
        //changeOrigin: true, // Needed for virtual hosted sites
      },
    }      
  },
  resolve: {
    alias: {
      'opendcs-api': resolve(__dirname, "../../java/api-clients/api-client-typescript/build/generated/openApi")
    }
  },
  build:
  {
    commonjsOptions:
    {
          include:[/opendcs-api/, /node_modules/],
    },
  },
})
