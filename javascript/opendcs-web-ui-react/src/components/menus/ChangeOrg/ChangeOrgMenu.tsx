import React from "react";
import { Button, Dropdown } from "react-bootstrap";
import { t } from "i18next";

interface ToggleProperties {
  org: string;
}

const OrgToggle: React.FC<ToggleProperties> = ({ org, ...args }) => {
  return (
    <Button {...args} size="lg">
      {org}
    </Button>
  );
};

export interface ChangeOrgMenuProperties {
  org: string;
  orgs: string[];
  onChange: (org: string) => void;
}

export const ChangeOrgMenu: React.FC<ChangeOrgMenuProperties> = ({
  org,
  orgs,
  onChange,
}) => {
  return (
    <Dropdown drop="start">
      <Dropdown.Toggle
        as={OrgToggle}
        aria-label={t("organization-settings")}
        org={org}
      />
      <Dropdown.Menu style={{ maxHeight: "300px", overflowY: "auto" }}>
        {orgs.map((org) => (
          <Dropdown.Item key={org} onClick={() => onChange(org)}>
            {org}
          </Dropdown.Item>
        ))}
      </Dropdown.Menu>
    </Dropdown>
  );
};
export default ChangeOrgMenu;
