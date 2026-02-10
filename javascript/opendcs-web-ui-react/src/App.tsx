import { useEffect } from "react";
import "./App.css";
import { useAuth } from "./contexts/app/AuthContext";
import Login from "./pages/auth/login";
import { TopBar, SideBar } from "./components/layout";
import { ModeIcons } from "./components/ModeIcon";

import { RESTAuthenticationAndAuthorizationApi, User } from "opendcs-api";
import { useApi } from "./contexts/app/ApiContext";
import { Route, Routes, useNavigate } from "react-router-dom";
import { Platforms } from "./pages/platforms";
import { Container } from "react-bootstrap";
import { Algorithms } from "./pages/computations/algorithms";
import { SitesPage } from "./pages/sites";

function App() {
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const api = useApi();

  useEffect(() => {
    const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);
    auth
      .checkSessionAuthorization()
      .then((value: User) => setUser(value))
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      .catch((_error: unknown) => navigate("/login"));
  }, [api.conf, setUser, navigate]);

  return (
    <>
      <ModeIcons />
      <TopBar />
      <Container fluid className="page-content d-flex">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<SideBar />}>
            <Route path="/platforms" element={<Platforms />} />
            <Route path="/sites" element={<SitesPage />} />
            <Route path="/algorithms" element={<Algorithms />} />
          </Route>
        </Routes>
      </Container>
    </>
  );
}

export default App;
