import { type ReactNode } from "react";
import { OrganizationsContext } from "./OrganizationsContext";
import { useOrganizationsQuery } from "../../queries/orgs";

interface ProviderProps {
  children: ReactNode;
}

export const OrganizationsProvider = ({ children }: ProviderProps) => {
  const { data: organizations = [] } = useOrganizationsQuery();
  return (
    <OrganizationsContext value={{ organizations }}>{children}</OrganizationsContext>
  );
};
