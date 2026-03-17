import { useEffect, useState, type ReactNode } from "react";
import { ApiOrganization, RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { useApi } from "./ApiContext";
import { OrganizationsContext } from "./OrganizationsContext";

interface ProviderProps {
  children: ReactNode;
}

export const OrganizationsProvider = ({ children }: ProviderProps) => {
  const api = useApi();
  const [organizations, setOrganizations] = useState<ApiOrganization[]>([]);

  useEffect(() => {
    const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);
    auth.getOrganizationsWithHttpInfo("").then((orgs) => {
      const data = orgs.data as ApiOrganization[];
      setOrganizations(data);
      // });
    });
  }, [api.conf]);

  return (
    <OrganizationsContext value={{ organizations }}>{children}</OrganizationsContext>
  );
};
