import React from "react";
import { Button, Dropdown } from "react-bootstrap";
import { PersonGear } from "react-bootstrap-icons";
import { useTranslation } from "react-i18next";
import type { User } from "../../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";

const UserToggle: React.FC = ({ ...args }) => {
  return (
    <Button {...args} size="lg">
      <PersonGear className="bi me-2 opacity-50 my-1 mode-icon-active" />
    </Button>
  );
};

export interface UserMenuProperties {
  user: User;
  logout: () => void;
}

export const UserMenu: React.FC<UserMenuProperties> = ({ user, logout }) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [t, _i18n] = useTranslation();
  return (
    <Dropdown drop="start">
      <Dropdown.Toggle as={UserToggle} aria-label={t("user-settings")} />
      <Dropdown.Menu>
        <Dropdown.Item>Profile - {user.email}</Dropdown.Item>
        <Dropdown.Item>Admin</Dropdown.Item>
        <Dropdown.Divider />
        <Dropdown.Item onClick={logout}>{t("translation:logout")}</Dropdown.Item>
      </Dropdown.Menu>
    </Dropdown>
  );
};

export default UserMenu;
