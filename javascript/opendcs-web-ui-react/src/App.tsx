import { useState } from "react";
import Login from "./pages/auth/login";
import { TopBar, SideBar, ProtectedRoute, PublicOnlyRoute } from "./components/layout";
import { ModeIcons } from "./components/ModeIcon";

import { Container } from "react-bootstrap";
import { Navigate, Route, Routes } from "react-router-dom";
import { Platforms } from "./pages/platforms";
import { Algorithms } from "./pages/computations/algorithms";
import { Computations } from "./pages/computations/computations";
import { SitesPage } from "./pages/sites";
import OidcCallback from "./pages/auth/login/OidcCallback";

function App() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <>
      <ModeIcons />
      <TopBar
        onToggleSidebar={() => setSidebarOpen((prev) => !prev)}
        sidebarOpen={sidebarOpen}
      />
      <Container fluid className="odcs-layout d-flex">
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<Login />} />
            <Route path="/oidc-callback" element={<OidcCallback />} />
          </Route>
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Navigate to="/platforms" replace />} />
            <Route
              element={
                <SideBar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
              }
            >
              <Route path="/platforms" element={<Platforms />} />
              <Route path="/sites" element={<SitesPage />} />
              <Route path="/computations" element={<Computations />} />
              <Route path="/algorithms" element={<Algorithms />} />
            </Route>
          </Route>
        </Routes>
      </Container>
    </>
  );
}

export default App;
