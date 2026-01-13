import { StrictMode, Suspense } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.tsx";
import "bootstrap/dist/css/bootstrap.min.css";
import "datatables.net-buttons-bs5/css/buttons.bootstrap5.css";
import "./main.css";
import "./assets/opendcs-shim.css";
import { ThemeProvider } from "./contexts/ThemeProvider.tsx";
import { AuthProvider } from "./contexts/AuthProvider.tsx";
import { ApiProvider } from "./contexts/ApiProvider.tsx";
import { BrowserRouter } from "react-router-dom";

import './i18n';

createRoot(document.getElementById("root")!).render(
  
  <StrictMode>
    <Suspense fallback="loading">
    <BrowserRouter>
      <ThemeProvider>
        <ApiProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </ApiProvider>
      </ThemeProvider>
    </BrowserRouter>
    </Suspense>
  </StrictMode>,
);
