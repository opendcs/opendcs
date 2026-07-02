import { StrictMode, Suspense } from "react";
import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./styles/main.scss";
import "datatables.net-bs5/css/dataTables.bootstrap5.css";
import "datatables.net-buttons-bs5/css/buttons.bootstrap5.css";
import "datatables.net-responsive-bs5";
import "bootstrap-icons/font/bootstrap-icons.css";
import { ThemeProvider } from "./contexts/app/ThemeProvider.tsx";
import { AuthProvider } from "./contexts/app/AuthProvider.tsx";
import { ApiProvider } from "./contexts/app/ApiProvider.tsx";
import { OrganizationsProvider } from "./contexts/app/OrganizationsProvider.tsx";
import { QueryProvider } from "./contexts/app/QueryProvider.tsx";
import { BrowserRouter } from "react-router-dom";

import "./i18n";
import { RefListProvider } from "./contexts/data/RefListProvider.tsx";
import { ToastStack } from "./components/notifications";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <Suspense fallback="loading">
      <BrowserRouter>
        <ThemeProvider>
          <ApiProvider>
            <AuthProvider>
              <QueryProvider>
                <OrganizationsProvider>
                  <RefListProvider>
                    <App />
                    <ToastStack />
                  </RefListProvider>
                </OrganizationsProvider>
              </QueryProvider>
            </AuthProvider>
          </ApiProvider>
        </ThemeProvider>
      </BrowserRouter>
    </Suspense>
  </StrictMode>,
);
