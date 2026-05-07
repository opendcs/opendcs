import { Button, Card, Col, Form, Row } from "react-bootstrap";
import type UserProperties from "./UserProperties";
import { useCallback, useEffect, useState, type ChangeEvent } from "react";

export function PasswordChange({
  user,
  updatePassword,
}: UserProperties & {
  updatePassword: (current: string, newPassword: string) => void;
}) {
  const changeChange: boolean =
    user.identityProviders?.find((idp) => idp.provider?.type === "BuiltIn") !==
    undefined;
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [repeatPassword, setRepeatPassword] = useState("");
  const [passwordsMatch, setPasswordsMatch] = useState(false);

  useEffect(() => {
    setPasswordsMatch(
      newPassword === repeatPassword && newPassword !== "" && currentPassword !== "",
    );
  }, [newPassword, repeatPassword, currentPassword]);

  const updateCurrentPassword = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      setCurrentPassword(event.target.value);
    },
    [setNewPassword],
  );

  const updateNewPassword = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      setNewPassword(event.target.value);
    },
    [setNewPassword],
  );

  const updateRepeatPassword = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      setRepeatPassword(event.target.value);
    },
    [setRepeatPassword],
  );

  return (
    changeChange && (
      <Card>
        <Card.Title className="d-flex">
          <span>Update Password</span>{" "}
        </Card.Title>
        <Card.Body>
          <Form
            onSubmit={(event) => {
              event.preventDefault();
              updatePassword(currentPassword, newPassword);
            }}
          >
            <Row>
              <Form.Group as={Row} controlId="currentPassword">
                <Form.Label column sm="1">
                  Current Password
                </Form.Label>
                <Col sm="auto">
                  <Form.Control
                    type="password"
                    value={currentPassword}
                    onChange={updateCurrentPassword}
                  />
                </Col>
              </Form.Group>
            </Row>
            <Row>
              <Form.Group as={Row} controlId="newPassword">
                <Form.Label column sm="1">
                  New Password
                </Form.Label>
                <Col sm="auto">
                  <Form.Control
                    type="password"
                    value={newPassword}
                    onChange={updateNewPassword}
                  />
                </Col>
              </Form.Group>
            </Row>
            <Row>
              <Form.Group as={Row} controlId="repeatPassword">
                <Form.Label column sm="1">
                  Repeat Password
                </Form.Label>
                <Col sm="auto">
                  <Form.Control
                    type="password"
                    value={repeatPassword}
                    onChange={updateRepeatPassword}
                  />
                </Col>
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
                Update Password
              </Button>
            </Row>
          </Form>
        </Card.Body>
      </Card>
    )
  );
}
