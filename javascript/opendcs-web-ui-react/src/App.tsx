import { useState } from "react";
import Login from "./pages/auth/login";
import { TopBar, SideBar, ProtectedRoute, PublicOnlyRoute } from "./components/layout";
import { ModeIcons } from "./components/ModeIcon";

import { Container } from "react-bootstrap";
import { Navigate, Route, Routes } from "react-router-dom";
import { Platforms } from "./pages/platforms";
import { Configs } from "./pages/decodes/configs";
import { Routing } from "./pages/decodes/routing";
import { DataSources } from "./pages/decodes/data-sources";
import { Netlists } from "./pages/decodes/netlists";
import { Presentations } from "./pages/decodes/presentations";
import { Schedules } from "./pages/schedule";
import { Algorithms } from "./pages/computations/algorithms";
import { Computations } from "./pages/computations/computations";
import { SitesPage } from "./pages/sites";
import { LoadingAppsPage } from "./pages/loading-apps";
import { EquipmentPage } from "./pages/equipment";
import OidcCallback from "./pages/auth/login/OidcCallback";
import UserProfilePage from "./pages/auth/user/UserProfilePage";

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
              <Route path="/configs" element={<Configs />} />
              <Route path="/routing" element={<Routing />} />
              <Route path="/data-sources" element={<DataSources />} />
              <Route path="/netlists" element={<Netlists />} />
              <Route path="/presentations" element={<Presentations />} />
              <Route path="/schedule" element={<Schedules />} />
              <Route path="/sites" element={<SitesPage />} />
              <Route path="/computations" element={<Computations />} />
              <Route path="/algorithms" element={<Algorithms />} />
              <Route path="/loading-apps" element={<LoadingAppsPage />} />
              <Route path="/equipment" element={<EquipmentPage />} />
              <Route path="/user/profile" element={<UserProfilePage />} />
            </Route>
          </Route>
        </Routes>
      </Container>
    </>
  );
}

export default App;
