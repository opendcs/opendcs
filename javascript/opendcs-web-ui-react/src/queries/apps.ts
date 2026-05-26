import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  RESTLoadingApplicationRecordsApi,
  type ApiAppRef,
  type ApiLoadingApp,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { appKeys } from "./keys";

const useAppsApi = () => {
  const api = useApi();
  const appApi = useMemo(
    () => new RESTLoadingApplicationRecordsApi(api.conf),
    [api.conf],
  );
  return { appApi, org: api.org };
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
    mutationFn: (app: ApiLoadingApp) => {
      const appId = app.appId && app.appId > 0 ? app.appId : undefined;
      return appApi.postApp(org, { ...app, appId });
    },
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: appKeys.all(org) });
      options?.onSuccess?.(...args);
    },
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
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: appKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};
