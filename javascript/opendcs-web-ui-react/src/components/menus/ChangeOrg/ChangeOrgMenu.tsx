import React from "react";
import { Button, Dropdown } from "react-bootstrap";
import { t } from "i18next";
import { ApiOrganization, RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { type ApiContextType, useApi } from "../../../contexts/app/ApiContext.ts";

interface ToggleProperties {
  org: ApiOrganization;
}

const OrgToggle: React.FC<ToggleProperties> = ({ org, ...args }) => {
  return (
    <Button {...args} size="lg">
      {org.name || "Change Organization"}
    </Button>
  );
};

export interface ChangeOrgMenuProperties {
  org: ApiOrganization;
  orgs: ApiOrganization[];
  changeOrg?: (
    org: ApiOrganization,
    api: ApiContextType,
    auth: RESTAuthenticationAndAuthorizationApi,
  ) => void;
}

const changeOrgFn = (
  org: ApiOrganization,
  api: ApiContextType,
  auth: RESTAuthenticationAndAuthorizationApi,
) => {
  auth
    .getOrganizations(org.name || "")
    .then(() => {
      api.setOrg(org);
      window.location.reload();
    })
    .catch(() => {
      alert("User does not have authorization for this organization.");
      if (api.orgObj === org) {
        api.setOrg({});
        window.location.reload();
      }
    });
};

export const ChangeOrgMenu: React.FC<ChangeOrgMenuProperties> = ({
  org,
  orgs,
  changeOrg,
}) => {
  const api = useApi();
  const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);
  const changeOrgFunc =
    changeOrg || ((org: ApiOrganization) => changeOrgFn(org, api, auth));
  return (
    <Dropdown drop="start">
      <Dropdown.Toggle
        as={OrgToggle}
        aria-label={t("organization-settings")}
        org={org}
      />
      <Dropdown.Menu style={{ maxHeight: "300px", overflowY: "auto" }}>
        {orgs.map((org) => (
          <Dropdown.Item key={org.name} onClick={() => changeOrgFunc(org, api, auth)}>
            {org.name}
          </Dropdown.Item>
        ))}
      </Dropdown.Menu>
    </Dropdown>
  );
};
export default ChangeOrgMenu;
