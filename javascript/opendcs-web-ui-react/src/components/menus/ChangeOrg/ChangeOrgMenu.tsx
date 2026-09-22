import React, { useMemo, useState } from "react";
import { Button, Dropdown, Modal } from "react-bootstrap";
import { t } from "i18next";
import {
  ApiOrganization,
  RESTAuthenticationAndAuthorizationApi,
  User,
} from "opendcs-api";
import { type ApiContextType, useApi } from "../../../contexts/app/ApiContext.ts";
import { useTranslation } from "react-i18next";
import { organizationTree } from "../../../util/orgHierarchy";

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
  user?: User;
  changeOrg?: (
    org: ApiOrganization,
    api: ApiContextType,
    auth: RESTAuthenticationAndAuthorizationApi,
  ) => void;
}

function hasRoles(org: string, user?: User): boolean {
  if (user) {
    return user.roles?.[org] != undefined;
  } else {
    return false;
  }
}

// Bootstrap already pads a dropdown entry by 1rem; each level of office
// nesting adds to that. Depth is unbounded, so this can't be a fixed set of
// CSS classes.
const indentFor = (depth: number) => `${1 + depth * 1.25}rem`;

export const ChangeOrgMenu: React.FC<ChangeOrgMenuProperties> = ({
  org,
  orgs,
  user,
  changeOrg,
}) => {
  const [t] = useTranslation();
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

  // Only offices the user holds a role in can be switched to, and those are
  // usually leaves — so the tree is pruned to branches containing one, and the
  // parent offices along the way come back as inert labels. Without them the
  // surviving districts would render as orphans with nothing to nest under.
  const orgEntries = useMemo(
    () => organizationTree(orgs, (candidate) => hasRoles(candidate.name!, user)),
    [orgs, user],
  );

  return (
    <>
      <Dropdown drop="start">
        <Dropdown.Toggle
          as={OrgToggle}
          aria-label={t("Change Organization")}
          org={org}
        />
        <Dropdown.Menu style={{ maxHeight: "300px", overflowY: "auto" }}>
          {orgEntries.map(({ org, depth, selectable }) =>
            selectable ? (
              <Dropdown.Item
                key={org.name}
                style={{ paddingLeft: indentFor(depth) }}
                onClick={() => changeOrgFunc(org, api, auth)}
              >
                {org.name}
              </Dropdown.Item>
            ) : (
              <Dropdown.Header key={org.name} style={{ paddingLeft: indentFor(depth) }}>
                {org.name}
              </Dropdown.Header>
            ),
          )}
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
