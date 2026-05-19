import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { RESTReferenceListsApi, type ApiRefList } from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { refListKeys } from "./keys";

// Ref lists are a per-org map (e.g. SiteNameType, TransportMediumType). Loaded
// once per org and shared across every consumer via the shared QueryClient
// cache — replaces the prior RefListProvider's bespoke ref-tracking.
export const useRefListsQuery = () => {
  const api = useApi();
  const refListApi = useMemo(() => new RESTReferenceListsApi(api.conf), [api.conf]);
  return useQuery<Record<string, ApiRefList>>({
    queryKey: refListKeys.list(api.org),
    queryFn: () => refListApi.getRefLists(api.org),
    // Reference lists rarely change within a session; keep them in cache long.
    staleTime: 60 * 60_000,
  });
};
