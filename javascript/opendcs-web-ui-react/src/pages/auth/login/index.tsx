import { type FormEvent, useState } from "react";
import { useAuth } from "../../../contexts/app/AuthContext";
import { Button, Card, Container, Form, Modal } from "react-bootstrap";
import { PersonCircle } from "react-bootstrap-icons";
import {
  ApiOrganization,
  Credentials,
  RESTAuthenticationAndAuthorizationApi,
} from "opendcs-api";
import { useApi } from "../../../contexts/app/ApiContext";
import { useOrganizations } from "../../../contexts/app/OrganizationsContext";
import { useNavigate, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";

export default function Login() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { setUser } = useAuth();
  const { organizations } = useOrganizations();
  const api = useApi();
  var errorMsg = "";
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

    const orgString: string = dataObject.organization.toString();
    const orgObj: ApiOrganization = orgString
      ? (JSON.parse(orgString) as ApiOrganization)
      : {};
    const org: string = orgObj.name || "";

    auth
      .postCredentials(org, credentials)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      .then((user_value: any) => {
        setUser(user_value);
        api.setOrg(orgObj);
        const redirectPath = location.state?.from || "/platforms";
        navigate(redirectPath, { replace: true });
      })
      .catch((error_: { toString: () => string }) => {
        setShowErrorModal(true);
        errorMsg = error_.toString();
      });
  }

  const [showErrorModal, setShowErrorModal] = useState(false);

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
              {organizations.length > 0 ? (
                <Form.Group className="mb-3">
                  <Form.Label>{t("organization")}</Form.Label>
                  <Form.Select id="organization" name="organization" required>
                    {organizations.map((org) => (
                      <option key={org.name} value={JSON.stringify(org)}>
                        {org.name}
                      </option>
                    ))}
                  </Form.Select>
                </Form.Group>
              ) : (
                <input type="hidden" name="organization" value="" />
              )}
              <div className="d-grid fade-in third">
                <Button variant="primary" type="submit" className="py-2">
                  {t("login")}
                </Button>
              </div>
            </Form>
          </Card.Body>
        </Card>
        <Modal show={showErrorModal} onHide={() => setShowErrorModal(false)} centered>
          <Modal.Header closeButton>
            <Modal.Title>{t("Login Failed")}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {t("user pass incorrect")}
            <br />
            {errorMsg}
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowErrorModal(false)}>
              {t("Close")}
            </Button>
          </Modal.Footer>
        </Modal>
      </Container>
    </Container>
  );
}
