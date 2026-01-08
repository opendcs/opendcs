import { useEffect } from "react";
import "./App.css";
import { useAuth } from "./contexts/AuthContext";
import Login from "./pages/auth/login";
import { TopBar, SideBar } from "./components/layout";
import { ModeIcons } from "./components/ModeIcon";

import { RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { useApi } from "./contexts/ApiContext";
import { Route, Routes, useNavigate } from "react-router-dom";
import { Platforms } from "./pages/platforms";
import { Sites } from "./pages/sites";
import { Container } from "react-bootstrap";
import { Algorithms } from "./pages/computations/algorithms";

function App() {
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const api = useApi();

  useEffect(() => {
    const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);
    auth
      .checkSessionAuthorization()
      // The current API spec (in the generated api) shows string as the return still
      // the endpoint *correctly* returns a user object. likely just an issue
      // with the autocomplete cache on my system.
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      .then((value: any) => setUser(value))
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
            <Route path="/sites" element={<Sites />} />
            <Route path="/algorithms" element={<Algorithms />} />
          </Route>
        </Routes>
      </Container>
    </>
  );
}

export default App;
