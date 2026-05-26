import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  RESTDECODESPlatformConfigurationsApi,
  RESTDECODESPlatformRecordsApi,
  type ApiConfigRef,
  type ApiPlatform,
  type ApiPlatformConfig,
  type ApiPlatformRef,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { platformKeys } from "./keys";

const usePlatformsApi = () => {
  const api = useApi();
  const platformApi = useMemo(
    () => new RESTDECODESPlatformRecordsApi(api.conf),
    [api.conf],
  );
  const configApi = useMemo(
    () => new RESTDECODESPlatformConfigurationsApi(api.conf),
    [api.conf],
  );
  return { platformApi, configApi, org: api.org };
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

export const usePlatformConfigsQuery = () => {
  const { configApi, org } = usePlatformsApi();
  return useQuery<ApiConfigRef[]>({
    queryKey: platformKeys.configList(org),
    queryFn: () => configApi.getConfigRefs(org),
  });
};

export const useFetchPlatformConfig = () => {
  const { configApi, org } = usePlatformsApi();
  const queryClient = useQueryClient();
  return (configId: number) =>
    queryClient.fetchQuery<ApiPlatformConfig>({
      queryKey: platformKeys.config(org, configId),
      queryFn: () => configApi.getConfig(org, configId),
    });
};

export const useSavePlatformMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiPlatform>, "mutationFn">,
) => {
  const { platformApi, org } = usePlatformsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (platform: ApiPlatform) => {
      const platformId =
        platform.platformId && platform.platformId > 0
          ? platform.platformId
          : undefined;
      return platformApi.postPlatform(org, { ...platform, platformId });
    },
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: platformKeys.all(org) });
      options?.onSuccess?.(...args);
    },
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
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: platformKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};
