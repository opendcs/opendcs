import React, { type FormEvent } from "react";
import { Form, Button } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import {
  RESTAuthenticationAndAuthorizationApi,
  Credentials,
  User,
  ApiOrganization,
} from "opendcs-api";
import { useApi } from "../../../contexts/app/ApiContext";
import { useOrganizations } from "../../../contexts/app/OrganizationsContext";

export interface FormLoginProperties {
  success: (user: User, org: ApiOrganization) => void;
  failure: (error: { toString: () => string }) => void;
}

export const FormLogin: React.FC<FormLoginProperties> = ({ success, failure }) => {
  const { t } = useTranslation();
  const { organizations } = useOrganizations();
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

    const org = JSON.parse(dataObject.organization.toString()) as ApiOrganization;

    auth
      .postCredentials(org.name || "", credentials)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      .then((user_value: any) => {
        success(user_value as User, org);
      })
      .catch((error_: { toString: () => string }) => {
        failure(error_);
      });
  }

  return (
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
  );
};

export default FormLogin;
