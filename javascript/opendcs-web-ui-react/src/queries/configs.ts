import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  RESTDECODESPlatformConfigurationsApi,
  type ApiConfigRef,
  type ApiPlatformConfig,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { configKeys } from "./keys";
import { invalidateThenDelegate } from "./mutationHelpers";

const useConfigsApi = () => {
  const api = useApi();
  const configApi = useMemo(
    () => new RESTDECODESPlatformConfigurationsApi(api.conf),
    [api.conf],
  );
  return { configApi, org: api.org };
};

export const useConfigsQuery = () => {
  const { configApi, org } = useConfigsApi();
  return useQuery<ApiConfigRef[]>({
    queryKey: configKeys.list(org),
    queryFn: () => configApi.getConfigRefs(org),
  });
};

export const useConfigQuery = (configId: number | undefined) => {
  const { configApi, org } = useConfigsApi();
  return useQuery<ApiPlatformConfig>({
    queryKey: configKeys.detail(org, configId ?? -1),
    queryFn: () => configApi.getConfig(org, configId!),
    enabled: configId !== undefined && configId > 0,
  });
};

// Imperative variant for renderDetail callers feeding React 19's `use()`.
// Reads from cache when fresh, falls back to network otherwise.
export const useFetchConfig = () => {
  const { configApi, org } = useConfigsApi();
  const queryClient = useQueryClient();
  return (configId: number) =>
    queryClient.fetchQuery<ApiPlatformConfig>({
      queryKey: configKeys.detail(org, configId),
      queryFn: () => configApi.getConfig(org, configId),
    });
};

export const useSaveConfigMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiPlatformConfig>, "mutationFn">,
) => {
  const { configApi, org } = useConfigsApi();
  const queryClient = useQueryClient();
  const invalidateList = invalidateThenDelegate<unknown, unknown, ApiPlatformConfig>(
    queryClient,
    configKeys.all(org),
    options?.onSuccess,
  );
  return useMutation({
    mutationFn: (config: ApiPlatformConfig) => {
      const configId =
        config.configId && config.configId > 0 ? config.configId : undefined;
      return configApi.postConfig(org, { ...config, configId });
    },
    ...options,
    onSuccess: async (...args) => {
      const variables = args[1];
      if (variables.configId != null && variables.configId > 0) {
        queryClient.removeQueries({
          queryKey: configKeys.detail(org, variables.configId),
        });
      }
      await invalidateList(...args);
    },
  });
};

export const useDeleteConfigMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { configApi, org } = useConfigsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (configId: number) => configApi.deleteConfig(org, configId),
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: configKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};
