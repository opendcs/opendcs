import { type FormEvent } from "react";
import { useAuth } from "../../../contexts/app/AuthContext";
import { Button, Card, Container, Form } from "react-bootstrap";
import { PersonCircle } from "react-bootstrap-icons";
import { Credentials, RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { useApi } from "../../../contexts/app/ApiContext";
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
    <Container className="odcs-login">
      <Container className="odcs-login__bg-image" />
      <Container className="odcs-login__card fade-in-down">
        <Card className="odcs-login__form-card">
          <Card.Body className="p-4 p-md-5">
            <div className="text-center mb-4 fade-in first">
              <PersonCircle className="odcs-login__avatar" />
              <h4 className="mt-3 fw-semibold">OpenDCS</h4>
              <p className="text-muted small">{t("login")}</p>
            </div>
            <Form onSubmit={handleLogin} className="fade-in second">
              <Form.Group className="mb-3">
                <Form.Label className="small fw-medium">{t("username")}</Form.Label>
                <Form.Control
                  type="text"
                  id="username"
                  required
                  name="username"
                  placeholder={t("username")}
                />
              </Form.Group>
              <Form.Group className="mb-4">
                <Form.Label className="small fw-medium">{t("password")}</Form.Label>
                <Form.Control
                  type="password"
                  id="password"
                  required
                  name="password"
                  placeholder={t("password")}
                />
              </Form.Group>
              <div className="d-grid fade-in third">
                <Button variant="primary" type="submit" className="py-2">
                  {t("login")}
                </Button>
              </div>
            </Form>
          </Card.Body>
        </Card>
      </Container>
    </Container>
  );
}
