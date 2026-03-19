import { type FormEvent, useState } from "react";
import { useAuth } from "../../../contexts/app/AuthContext";
import { Button, Card, Container, Form, Modal } from "react-bootstrap";
import { PersonCircle } from "react-bootstrap-icons";
import {
  ApiOrganization,
  Credentials,
  RESTAuthenticationAndAuthorizationApi,
  User,
} from "opendcs-api";
import { useApi } from "../../../contexts/app/ApiContext";
import { useOrganizations } from "../../../contexts/app/OrganizationsContext";
import { useNavigate, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import FormLogin from "./FormLogin";

export default function Login() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { setUser } = useAuth();
  const { organizations } = useOrganizations();
  const api = useApi();
  const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);
  let errorMsg = "";

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
            <FormLogin
              login={(credentials: Credentials) => {
                auth
                  .postCredentials(api.org, credentials)
                  // eslint-disable-next-line @typescript-eslint/no-explicit-any
                  .then((user_value: any) => {
                    setUser(user_value);
                    const redirectPath = location.state?.from || "/platforms";
                    navigate(redirectPath, { replace: true });
                  })
                  .catch((error_: { toString: () => string }) => {
                    errorMsg = error_.toString();
                    setShowErrorModal(true);
                  });
              }}
            />
            {organizations.length > 0 ? (
              <Form.Group className="mb-3">
                <Form.Label>{t("organization")}</Form.Label>
                <Form.Select
                  id="organization"
                  name="organization"
                  required
                  defaultValue={api.org}
                  onChange={(e) => {
                    api.setOrg(JSON.parse(e.currentTarget.value));
                  }}
                >
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
