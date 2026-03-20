import React, { type FormEvent } from "react";
import { Form, Button } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { Credentials } from "opendcs-api";

export interface FormLoginProperties {
  login: (credentials: Credentials) => void;
}

export const FormLogin: React.FC<FormLoginProperties> = ({ login }) => {
  const { t } = useTranslation();

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

    login(credentials);
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
      <div className="d-grid fade-in third">
        <Button variant="primary" type="submit" className="py-2">
          {t("login")}
        </Button>
      </div>
    </Form>
  );
};

export default FormLogin;
