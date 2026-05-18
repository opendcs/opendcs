import { useEffect, useRef, useState } from "react";
import { useAuth } from "../../../contexts/app/AuthContext";
import { Button, Card, Container, Form, Modal } from "react-bootstrap";
import { PersonCircle } from "react-bootstrap-icons";
import { type Credentials, RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { useApi } from "../../../contexts/app/ApiContext";
import { useOrganizations } from "../../../contexts/app/OrganizationsContext";
import { useNavigate, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import FormLogin from "./FormLogin";
import {
  type FormScheme,
  type OidcScheme,
  type Values,
} from "../../../util/login-providers/Scheme.types";
import { oidcConfigToClient, type ParamMap } from "../../../util/login-providers";
import type { SigninRequest } from "oidc-client-ts";

export default function Login() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { loginSchemes, setUser } = useAuth();
  const { organizations } = useOrganizations();
  const api = useApi();
  const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);
  const [showErrorModal, setShowErrorModal] = useState(false);
  const inputOptions = useRef<ParamMap>({});

  let errorMsg = "";

  useEffect(() => {
    if (location.state?.errorMsg) {
      errorMsg = location.state!.errorMsg;
      setShowErrorModal(true);
    }
  }, [location.state]);

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
            {Object.entries(loginSchemes ?? {}).map(([key, scheme]) => {
              inputOptions.current = {
                ...inputOptions.current,
                [key]: {},
              };
              if (scheme.formConfig as FormScheme) {
                return (
                  <FormLogin
                    key={key}
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
                    loginOptions={scheme as FormScheme}
                  />
                );
              } else {
                return (
                  <Container className="w-100 mt-2 mb-2 p-0">
                    <Button
                      key={key}
                      variant="primary"
                      className="w-100"
                      onClick={(event) => {
                        event.preventDefault();
                        const queryParameters = inputOptions.current[key];
                        const client = oidcConfigToClient(
                          scheme as OidcScheme,
                          inputOptions.current[key],
                        );
                        const req = client.createSigninRequest({});
                        req.then((r: SigninRequest) => {
                          sessionStorage.setItem(r.state.id, client.settings.client_id);
                          sessionStorage.setItem(
                            "queryParameters",
                            JSON.stringify(queryParameters),
                          );
                          const oidcSessionInfo = {
                            state: r.state.id,
                            provider: key,
                            queryParameters: queryParameters,
                            redirectAfterAuth: new URL(
                              location.state?.from || "/platforms",
                              globalThis.location.origin,
                            ).toString(),
                          };
                          document.cookie = `oidcInfo=${encodeURIComponent(JSON.stringify(oidcSessionInfo))}; path=/odcsapi; max-age=300; SameSite=Lax`;

                          globalThis.location.href = r.url;
                        });
                      }}
                    >
                      Login with {key}
                    </Button>
                    {scheme.queryParameters &&
                      Object.entries(scheme.queryParameters)

                        .filter((qp) => {
                          console.log(qp);
                          return qp[1] as unknown as Values;
                        })
                        .map((qp) => [qp[0], qp[1]] as unknown as [string, string[]])
                        .map(([qp, values]) => {
                          if (values.length >= 0) {
                            inputOptions.current[key] = {
                              ...inputOptions.current[key],
                              [qp]: values[0],
                            };
                          }
                          return (
                            <Form.Group className="mb-3">
                              <Form.Label>{qp}</Form.Label>
                              <Form.Select
                                name={`${qp}`}
                                id={`queryParam_${qp}`}
                                onChange={(event) =>
                                  (inputOptions.current[key] = {
                                    ...inputOptions.current[key],
                                    [qp]: event.target.value,
                                  })
                                }
                              >
                                {values.map((v, i) => (
                                  <option key={`${qp}_${i}`} value={`${v}`}>
                                    {v}
                                  </option>
                                ))}
                              </Form.Select>
                            </Form.Group>
                          );
                        })}
                  </Container>
                );
              }
            })}
            {organizations.length > 0 ? (
              <Form.Group className="mb-3">
                <Form.Label id="organization-label">{t("organization")}</Form.Label>
                <Form.Select
                  id="organization"
                  name="organization"
                  aria-labelledby="organization-label"
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
