import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  RESTDECODESPlatformRecordsApi,
  type ApiPlatform,
  type ApiPlatformRef,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { platformKeys } from "./keys";
import { invalidateThenDelegate, normalizeNewId } from "./mutationHelpers";

const usePlatformsApi = () => {
  const api = useApi();
  const platformApi = useMemo(
    () => new RESTDECODESPlatformRecordsApi(api.conf),
    [api.conf],
  );
  return { platformApi, org: api.org };
};

export const usePlatformsQuery = () => {
  const { platformApi, org } = usePlatformsApi();
  return useQuery<ApiPlatformRef[]>({
    queryKey: platformKeys.list(org),
    queryFn: () => platformApi.getPlatformRefs(org),
  });
};

export const usePlatformQuery = (platformId: number | undefined) => {
  const { platformApi, org } = usePlatformsApi();
  return useQuery<ApiPlatform>({
    queryKey: platformKeys.detail(org, platformId ?? -1),
    queryFn: () => platformApi.getPlatform(org, platformId!),
    enabled: platformId !== undefined && platformId > 0,
  });
};

// Imperative variant for PlatformsTable.renderDetail, which feeds React 19's
// `use()`. Reads from cache when fresh, falls back to network otherwise.
export const useFetchPlatform = () => {
  const { platformApi, org } = usePlatformsApi();
  const queryClient = useQueryClient();
  return (platformId: number) =>
    queryClient.fetchQuery<ApiPlatform>({
      queryKey: platformKeys.detail(org, platformId),
      queryFn: () => platformApi.getPlatform(org, platformId),
    });
};

export const useSavePlatformMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiPlatform>, "mutationFn">,
) => {
  const { platformApi, org } = usePlatformsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (platform: ApiPlatform) =>
      platformApi.postPlatform(org, {
        ...platform,
        platformId: normalizeNewId(platform.platformId),
      }),
    ...options,
    onSuccess: invalidateThenDelegate(
      queryClient,
      platformKeys.all(org),
      options?.onSuccess,
    ),
  });
};

export const useDeletePlatformMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { platformApi, org } = usePlatformsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (platformId: number) => platformApi.deletePlatform(org, platformId),
    ...options,
    onSuccess: invalidateThenDelegate(
      queryClient,
      platformKeys.all(org),
      options?.onSuccess,
    ),
  });
};
