import { createContext, useContext } from "react";
import { ApiOrganization } from "opendcs-api";
import { useTranslation } from "react-i18next";

export interface OrganizationsContextType {
  organizations: ApiOrganization[];
}

export const parseOrg = (orgString: string | null) => {
  return JSON.parse(orgString ?? "{}") as ApiOrganization;
};

export const OrganizationsContext = createContext<OrganizationsContextType>({
  organizations: [],
});

export const useOrganizations = () => {
  const { t } = useTranslation();
  const context = useContext(OrganizationsContext);
  if (context === undefined) {
    throw new Error(t("Organizations not defined"));
  }
  return context;
};
