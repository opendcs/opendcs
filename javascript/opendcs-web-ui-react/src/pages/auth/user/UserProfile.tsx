import { Card, Stack } from "react-bootstrap";
import { useAuth } from "../../../contexts/app/AuthContext";
import TimeInfo from "./TimeInfo";
import { Roles } from "./Roles";
import { PasswordChange } from "./PasswordChange";

export interface UserProfileProperties {
  updatePassword: (currentPassword: string, newPassword: string) => void;
}

export const UserProfile: React.FC<UserProfileProperties> = ({ updatePassword }) => {
  const { user } = useAuth();

  if (user === undefined) {
    return;
  }

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

export default UserProfile;
