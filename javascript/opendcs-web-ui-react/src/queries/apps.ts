import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  OpenDCSProcessMonitorAndControlAPPApi,
  RESTLoadingApplicationRecordsApi,
  type ApiAppRef,
  type ApiAppStatus,
  type ApiLoadingApp,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { appKeys } from "./keys";
import { invalidateThenDelegate, normalizeNewId } from "./mutationHelpers";

const useAppsApi = () => {
  const api = useApi();
  const appApi = useMemo(
    () => new RESTLoadingApplicationRecordsApi(api.conf),
    [api.conf],
  );
  return { appApi, org: api.org };
};

const useAppMonitorApi = () => {
  const api = useApi();
  const monitorApi = useMemo(
    () => new OpenDCSProcessMonitorAndControlAPPApi(api.conf),
    [api.conf],
  );
  return { monitorApi, org: api.org };
};

// App refs power the "process" dropdown on the Computations editor and the
// Loading Apps table. The list is small and rarely changes.
export const useAppRefsQuery = () => {
  const { appApi, org } = useAppsApi();
  return useQuery<ApiAppRef[]>({
    queryKey: appKeys.list(org),
    queryFn: () => appApi.getAppRefs(org),
  });
};

// Imperative variant for LoadingAppsTable.renderDetail, which feeds React 19's
// `use()`. Reads from cache when fresh, falls back to network otherwise.
export const useFetchApp = () => {
  const { appApi, org } = useAppsApi();
  const queryClient = useQueryClient();
  return (appId: number) =>
    queryClient.fetchQuery<ApiLoadingApp>({
      queryKey: [...appKeys.all(org), "detail", appId] as const,
      queryFn: () => appApi.getApp(org, appId),
    });
};

export const useSaveAppMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiLoadingApp>, "mutationFn">,
) => {
  const { appApi, org } = useAppsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (app: ApiLoadingApp) =>
      appApi.postApp(org, { ...app, appId: normalizeNewId(app.appId) }),
    ...options,
    onSuccess: invalidateThenDelegate(
      queryClient,
      appKeys.all(org),
      options?.onSuccess,
    ),
  });
};

export const useDeleteAppMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { appApi, org } = useAppsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (appId: number) => appApi.deleteApp(org, appId),
    ...options,
    onSuccess: invalidateThenDelegate(
      queryClient,
      appKeys.all(org),
      options?.onSuccess,
    ),
  });
};

export const useAppStatQuery = () => {
  const { monitorApi, org } = useAppMonitorApi();
  return useQuery<ApiAppStatus[]>({
    queryKey: appKeys.stat(org),
    queryFn: async () => {
      try {
        return await monitorApi.getAppStat(org);
      } catch {
        return [];
      }
    },
    refetchInterval: 30_000,
  });
};
