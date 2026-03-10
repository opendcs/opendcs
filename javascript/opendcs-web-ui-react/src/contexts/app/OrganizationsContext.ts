import { createContext, useContext } from "react";

export interface OrganizationsContextType {
  organizations: string[];
}

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
