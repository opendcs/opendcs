import { Card, Stack } from "react-bootstrap";
import { useAuth } from "../../../contexts/app/AuthContext";
import TimeInfo from "./TimeInfo";
import { Roles } from "./Roles";
import { PasswordChange } from "./PasswordChange";
import { useApi } from "../../../contexts/app/ApiContext";
import { useCallback } from "react";
import { UserOperationsApi } from "opendcs-api";

export const UserProfilePage: React.FC = () => {
  const { user } = useAuth();
  const { conf } = useApi();

  const updatePassword = useCallback(
    async (currentPassword: string, newPassword: string): Promise<boolean> => {
      const userApi = new UserOperationsApi(conf);
      return userApi
        .updatePassword("", {
          currentPassword: currentPassword,
          newPassword: newPassword,
        })
        .then(() => true)
        .catch((err) => {
          console.error("Failed to update password", err);
          return false;
        });
    },
    [conf],
  );

  if (user === undefined) {
    return;
  }

  return (
    <Card>
      <Card.Title>{user.email}</Card.Title>
      <Card.Body>
        <Stack gap={2}>
          <TimeInfo user={user} key="time" />
          <PasswordChange user={user} updatePassword={updatePassword} key="pw" />
          <Roles user={user} key="roles" />
        </Stack>
      </Card.Body>
    </Card>
  );
};

export default UserProfilePage;
