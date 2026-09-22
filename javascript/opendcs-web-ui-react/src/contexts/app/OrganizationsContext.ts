import { createContext, useContext, useMemo } from "react";
import { ApiOrganization } from "opendcs-api";
import { compareStrings } from "../../util/sort";

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
  // Sorted on read rather than in useOrganizationsQuery so that every consumer
  // gets an alphabetized list no matter where the data came from — the query,
  // or a value handed straight to OrganizationsContext (stories and tests do
  // exactly that, which is why a sort in the query alone was invisible).
  const organizations = useMemo(
    () => [...context.organizations].sort((a, b) => compareStrings(a.name, b.name)),
    [context.organizations],
  );
  // Memoized so the returned object keeps a stable identity across renders —
  // consumers are free to put it in a dependency array.
  return useMemo(() => ({ ...context, organizations }), [context, organizations]);
};
