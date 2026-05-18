import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  RESTAuthenticationAndAuthorizationApi,
  type ApiOrganization,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { orgKeys } from "./keys";

// The org list is global (passes "" as the org header) — it's the input to
// the org switcher itself, so it must not be scoped by current org.
export const useOrganizationsQuery = () => {
  const api = useApi();
  const authApi = useMemo(
    () => new RESTAuthenticationAndAuthorizationApi(api.conf),
    [api.conf],
  );
  return useQuery<ApiOrganization[]>({
    queryKey: orgKeys.list(),
    queryFn: async () => {
      const res = await authApi.getOrganizationsWithHttpInfo("");
      return res.data as ApiOrganization[];
    },
    staleTime: 60 * 60_000,
  });
};
