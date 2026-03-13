import { createContext, useContext } from "react";
import { ApiOrganization } from "opendcs-api";

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
  const context = useContext(OrganizationsContext);
  if (context === undefined) {
    throw new Error("Organizations not defined");
  }
  return context;
};
