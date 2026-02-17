import "./App.css";
import Login from "./pages/auth/login";
import { TopBar, SideBar, ProtectedRoute, PublicOnlyRoute } from "./components/layout";
import { ModeIcons } from "./components/ModeIcon";

import { Navigate, Route, Routes } from "react-router-dom";
import { Platforms } from "./pages/platforms";
import { Container } from "react-bootstrap";
import { Algorithms } from "./pages/computations/algorithms";
import { SitesPage } from "./pages/sites";

function App() {
  return (
    <>
      <ModeIcons />
      <TopBar />
      <Container fluid className="page-content d-flex">
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<Login />} />
          </Route>
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Navigate to="/platforms" replace />} />
            <Route element={<SideBar />}>
              <Route path="/platforms" element={<Platforms />} />
              <Route path="/sites" element={<SitesPage />} />
              <Route path="/algorithms" element={<Algorithms />} />
            </Route>
          </Route>
        </Routes>
      </Container>
    </>
  );
}

export default App;
