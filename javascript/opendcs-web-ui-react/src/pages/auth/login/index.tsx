import { type FormEvent } from "react";
import { useAuth } from "../../../contexts/AuthContext";
import { Button, Container, Form, Image } from "react-bootstrap";
import "./Login.css";
import { Credentials, RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { useApi } from "../../../contexts/ApiContext";
import { useNavigate, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";

export default function Login() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { setUser } = useAuth();
  const api = useApi();

  const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);

  function handleLogin(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    event.stopPropagation();
    const form = event.currentTarget;

    const formData = new FormData(form);

    const dataObject = Object.fromEntries(formData.entries());
    // Convert FormData to a plain object for easier use
    const credentials: Credentials = {
      username: dataObject.username.toString(),
      password: dataObject.password.toString(),
    };

    auth
      .postCredentials("", credentials)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      .then((user_value: any) => {
        setUser(user_value);
        const redirectPath = location.state?.from || "/platforms";
        navigate(redirectPath, { replace: true });
      })
      .catch((error_: { toString: () => string }) =>
        alert("Login failed" + error_.toString()),
      );
  }

  return (
    <Container className="page-content d-flex" fluid>
      <Container className="content-wrapper" fluid>
        <Container className="content loginPageBackground" fluid>
          <Container className="wrapper fadeInDown" fluid>
            <Container id="formContent" className="slightOpacity" fluid>
              <div className="fadeIn first">
                <Image src="/user_profile_image_large.png" id="icon" alt="User icon" />
              </div>
              <Form onSubmit={handleLogin}>
                <Form.Group className="mb-3">
                  <Form.Label>{t("username")}</Form.Label>
                  <Form.Control type="text" id="username" required name="username" />
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Label>{t("password")}</Form.Label>
                  <Form.Control
                    type="password"
                    id="password"
                    required
                    name="password"
                  />
                </Form.Group>
                <Button variant="primary" type="submit">
                  {t("login")}
                </Button>
              </Form>
            </Container>
          </Container>
        </Container>
      </Container>
    </Container>
  );
}
