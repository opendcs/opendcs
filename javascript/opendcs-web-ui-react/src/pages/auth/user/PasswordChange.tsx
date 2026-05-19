import { Button, Card, Col, Form, Row } from "react-bootstrap";
import type UserProperties from "./UserProperties";
import { useCallback, useEffect, useState, type ChangeEvent } from "react";
import { useTranslation } from "react-i18next";

export function PasswordChange({
  user,
  updatePassword,
}: UserProperties & {
  updatePassword: (current: string, newPassword: string) => Promise<boolean>;
}) {
  const canChange: boolean =
    user.identityProviders?.find((idp) => idp.provider?.type === "BuiltIn") !==
    undefined;
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState<string>("");
  const [repeatPassword, setRepeatPassword] = useState<string>("");
  const [passwordsMatch, setPasswordsMatch] = useState<boolean>(false);
  const [success, setSuccess] = useState<boolean | null>(null);
  const [validated, setValidated] = useState<boolean>(false);
  const { t } = useTranslation(["user-data"]);

  useEffect(() => {
    setPasswordsMatch(
      newPassword === repeatPassword && newPassword !== "" && currentPassword !== "",
    );
  }, [newPassword, repeatPassword, currentPassword]);

  useEffect(() => {
    setValidated(passwordsMatch);
  }, [passwordsMatch]);

  useEffect(() => {
    if (success === true) {
      setCurrentPassword("");
      setNewPassword("");
      setRepeatPassword("");
      setValidated(false);
    } else if (success === false) {
      setValidated(false);
    }
  }, [success]);

  const updateCurrentPassword = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      setCurrentPassword(event.target.value);
      setSuccess(null);
    },
    [setNewPassword],
  );

  const updateNewPassword = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      setNewPassword(event.target.value);
      setSuccess(null);
    },
    [setNewPassword],
  );

  const updateRepeatPassword = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      setRepeatPassword(event.target.value);
      setSuccess(null);
    },
    [setRepeatPassword],
  );

  return (
    canChange && (
      <Card>
        <Card.Title className="d-flex">{t("password_change.title")}</Card.Title>
        <Card.Body>
          <Form
            noValidate
            validated={validated}
            onSubmit={(event) => {
              event.preventDefault();
              const form = event.currentTarget;
              if (form.checkValidity()) {
                updatePassword(currentPassword, newPassword).then((value) =>
                  setSuccess(value),
                );
              }
              setValidated(true);
            }}
          >
            <Row>
              <Form.Group as={Row} controlId="currentPassword">
                <Form.Label column sm="1">
                  {t("password_change.current_password")}
                </Form.Label>
                <Col sm="auto">
                  <Form.Control
                    required
                    type="password"
                    value={currentPassword}
                    onChange={updateCurrentPassword}
                  />
                </Col>
                <Col sm="auto" />
              </Form.Group>
            </Row>
            <Row>
              <Form.Group as={Row} controlId="newPassword">
                <Form.Label column sm="1">
                  {t("password_change.new_password")}
                </Form.Label>
                <Col sm="auto">
                  <Form.Control
                    required
                    minLength={8}
                    type="password"
                    value={newPassword}
                    onChange={updateNewPassword}
                  />
                </Col>
                <Col sm="auto">
                  {success === true && <>{t("password_change.success")}</>}
                  {success === false && <>{t("password_change.failed")}</>}
                </Col>
              </Form.Group>
            </Row>
            <Row>
              <Form.Group as={Row} controlId="repeatPassword">
                <Form.Label column sm="1">
                  {t("password_change.repeat_password")}
                </Form.Label>
                <Col sm="auto">
                  <Form.Control
                    required
                    type="password"
                    value={repeatPassword}
                    onChange={updateRepeatPassword}
                  />
                </Col>
                <Col sm="auto" />
              </Form.Group>
            </Row>
            <Row>
              <Button
                type="submit"
                disabled={!passwordsMatch}
                className="ml-auto align-self-end"
                variant="warning"
                size="lg"
              >
                {t("password_change.update_password")}
              </Button>
            </Row>
          </Form>
        </Card.Body>
      </Card>
    )
  );
}
