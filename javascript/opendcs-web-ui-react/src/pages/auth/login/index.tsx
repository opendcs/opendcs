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
  let errorMsg = "";

  // const orgString: string = dataObject.organization.toString();
  // const orgObj: ApiOrganization = orgString
  //   ? (JSON.parse(orgString) as ApiOrganization)
  //   : {};
  // const org: string = orgObj.name || "";

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
              success={(user: User, org: ApiOrganization) => {
                setUser(user);
                api.setOrg(org);
                const redirectPath = location.state?.from || "/platforms";
                navigate(redirectPath, { replace: true });
              }}
              failure={(error_: { toString: () => string }) => {
                setShowErrorModal(true);
                errorMsg = error_.toString();
              }}
            />
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
