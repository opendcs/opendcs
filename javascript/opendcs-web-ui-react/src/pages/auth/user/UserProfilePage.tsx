import { Card, Stack } from "react-bootstrap";
import { useAuth } from "../../../contexts/app/AuthContext";
import TimeInfo from "./TimeInfo";
import { Roles } from "./Roles";
import { PasswordChange } from "./PasswordChange";
import { useApi } from "../../../contexts/app/ApiContext";
import { useCallback } from "react";
import { UserOperationsApi } from "opendcs-api";

export interface UserProfileProperties {}

export const UserProfilePage: React.FC<UserProfileProperties> = ({}) => {
  const { user } = useAuth();
  const { conf } = useApi();

  if (user === undefined) {
    return;
  }

  const updatePassword = useCallback(
    async (currentPassword: string, newPassword: string) => {
      const userApi = new UserOperationsApi(conf);
      userApi
        .updatePassword("", {
          currentPassword: currentPassword,
          newPassword: newPassword,
        })
        .then((res) => {
          console.log(res);
        })
        .catch((err) => console.log(err)); // TODO: proper error handling
    },
    [conf],
  );

  return (
    <Card>
      <Card.Title>{user.email}</Card.Title>
      <Card.Body>
        <Stack gap={2}>
          <TimeInfo user={user} />
          <PasswordChange user={user} updatePassword={updatePassword} />
          <Roles user={user} />
        </Stack>
      </Card.Body>
    </Card>
  );
};

export default UserProfilePage;
