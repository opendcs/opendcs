import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import 'bootstrap/dist/css/bootstrap.min.css'
import './main.css'
import './assets/opendcs-shim.css'
import { ThemeProvider } from './contexts/ThemeProvider.tsx'
import { AuthProvider } from './contexts/AuthProvider.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <AuthProvider>
        <App />
    </AuthProvider>
    </ThemeProvider>
  </StrictMode>,
)
