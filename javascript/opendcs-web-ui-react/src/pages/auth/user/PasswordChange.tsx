import { Button, Card, Col, Form, Row } from "react-bootstrap";
import type UserProperties from "./UserProperties";
import { useCallback, useState, type ChangeEvent } from "react";
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
  const [success, setSuccess] = useState<boolean | null>(null);
  const [validated, setValidated] = useState<boolean>(false);
  const { t } = useTranslation(["user-data"]);

  const passwordsMatch =
    newPassword === repeatPassword && newPassword !== "" && currentPassword !== "";

  const updateCurrentPassword = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value;
      setCurrentPassword(value);
      setSuccess(null);
      setValidated(
        newPassword === repeatPassword && newPassword !== "" && value !== "",
      );
    },
    [newPassword, repeatPassword],
  );

  const updateNewPassword = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value;
      setNewPassword(value);
      setSuccess(null);
      setValidated(value === repeatPassword && value !== "" && currentPassword !== "");
    },
    [repeatPassword, currentPassword],
  );

  const updateRepeatPassword = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value;
      setRepeatPassword(value);
      setSuccess(null);
      setValidated(
        newPassword === value && newPassword !== "" && currentPassword !== "",
      );
    },
    [newPassword, currentPassword],
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
                updatePassword(currentPassword, newPassword).then((value) => {
                  setSuccess(value);
                  if (value) {
                    setCurrentPassword("");
                    setNewPassword("");
                    setRepeatPassword("");
                  }
                  setValidated(false);
                });
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
