import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import { RESTDECODESSiteRecordsApi, type ApiSite, type ApiSiteRef } from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { siteKeys } from "./keys";
import { invalidateThenDelegate } from "./mutationHelpers";

const useSitesApi = () => {
  const api = useApi();
  const sitesApi = useMemo(() => new RESTDECODESSiteRecordsApi(api.conf), [api.conf]);
  return { sitesApi, org: api.org };
};

export const useSitesQuery = () => {
  const { sitesApi, org } = useSitesApi();
  return useQuery<ApiSiteRef[]>({
    queryKey: siteKeys.list(org),
    queryFn: () => sitesApi.getsiterefs(org),
  });
};

export const useSiteQuery = (siteId: number | undefined) => {
  const { sitesApi, org } = useSitesApi();
  return useQuery<ApiSite>({
    queryKey: siteKeys.detail(org, siteId ?? -1),
    queryFn: () => sitesApi.getsite(org, siteId!),
    enabled: siteId !== undefined && siteId > 0,
  });
};

// Imperative variant for callers that hand a Promise<ApiSite> downstream
// (e.g. SitesTable.renderDetail, which feeds React 19's `use()`). Reads from
// cache when fresh, falls back to network otherwise.
export const useFetchSite = () => {
  const { sitesApi, org } = useSitesApi();
  const queryClient = useQueryClient();
  return (siteId: number) =>
    queryClient.fetchQuery<ApiSite>({
      queryKey: siteKeys.detail(org, siteId),
      queryFn: () => sitesApi.getsite(org, siteId),
    });
};

export const useSaveSiteMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiSite>, "mutationFn">,
) => {
  const { sitesApi, org } = useSitesApi();
  const queryClient = useQueryClient();
  const invalidateList = invalidateThenDelegate(
    queryClient,
    siteKeys.all(org),
    options?.onSuccess,
  );
  return useMutation({
    mutationFn: (site: ApiSite) => sitesApi.postsite(org, site),
    ...options,
    onSuccess: async (data, variables, context) => {
      if (variables.siteId != null && variables.siteId > 0) {
        queryClient.removeQueries({
          queryKey: siteKeys.detail(org, variables.siteId),
        });
      }
      await invalidateList(data, variables, context);
    },
  });
};

export const useDeleteSiteMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { sitesApi, org } = useSitesApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (siteId: number) => sitesApi.deletesite(org, siteId),
    ...options,
    onSuccess: invalidateThenDelegate(
      queryClient,
      siteKeys.all(org),
      options?.onSuccess,
    ),
  });
};
