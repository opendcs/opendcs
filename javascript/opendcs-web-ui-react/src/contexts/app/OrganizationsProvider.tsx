import { useEffect, useState, type ReactNode } from "react";
import { RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { useApi } from "./ApiContext";
import { OrganizationsContext } from "./OrganizationsContext";

interface ProviderProps {
  children: ReactNode;
}

export const OrganizationsProvider = ({ children }: ProviderProps) => {
  const api = useApi();
  const [organizations, setOrganizations] = useState<string[]>([]);

  useEffect(() => {
    const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);
    auth.getOrganizationsWithHttpInfo("").then((orgs) => {
      orgs.body.text().then((text) => {
        const orgBody: { name: string }[] = JSON.parse(text);
        setOrganizations(orgBody.map((org) => org.name));
      });
    });
  }, [api.conf]);

  return (
    <OrganizationsContext value={{ organizations }}>{children}</OrganizationsContext>
  );
};
