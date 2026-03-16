import React, { useState } from "react";
import { Button, Dropdown, Modal } from "react-bootstrap";
import { t } from "i18next";
import { ApiOrganization, RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { type ApiContextType, useApi } from "../../../contexts/app/ApiContext.ts";

interface ToggleProperties {
  org: ApiOrganization;
}

const OrgToggle: React.FC<ToggleProperties> = ({ org, ...args }) => {
  return (
    <Button {...args} size="lg">
      {org.name || t("Change Organization")}
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

export const ChangeOrgMenu: React.FC<ChangeOrgMenuProperties> = ({
  org,
  orgs,
  changeOrg,
}) => {
  const api = useApi();
  const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);
  const [showErrorModal, setShowErrorModal] = useState(false);
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
        setShowErrorModal(true);
        if (api.orgObj === org) {
          api.setOrg({});
          window.location.reload();
        }
      });
  };

  const changeOrgFunc =
    changeOrg || ((org: ApiOrganization) => changeOrgFn(org, api, auth));

  return (
    <>
      <Dropdown drop="start">
        <Dropdown.Toggle
          as={OrgToggle}
          aria-label={"organization-settings"}
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

      <Modal show={showErrorModal} onHide={() => setShowErrorModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>{t("Authorization Error")}</Modal.Title>
        </Modal.Header>
        <Modal.Body>{t("User not authorized")}</Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowErrorModal(false)}>
            {t("Close")}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
};
export default ChangeOrgMenu;
