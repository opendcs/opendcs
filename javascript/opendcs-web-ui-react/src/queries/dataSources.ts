import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  RESTDECODESDataSourceRecordsApi,
  type ApiDataSource,
  type ApiDataSourceRef,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { dataSourceKeys } from "./keys";

const useDataSourcesApi = () => {
  const api = useApi();
  const dataSourceApi = useMemo(
    () => new RESTDECODESDataSourceRecordsApi(api.conf),
    [api.conf],
  );
  return { dataSourceApi, org: api.org };
};

export const useDataSourceRefsQuery = () => {
  const { dataSourceApi, org } = useDataSourcesApi();
  return useQuery<ApiDataSourceRef[]>({
    queryKey: dataSourceKeys.list(org),
    queryFn: () => dataSourceApi.getDataSourceRefs(org),
  });
};

export const useDataSourceQuery = (dataSourceId: number | undefined) => {
  const { dataSourceApi, org } = useDataSourcesApi();
  return useQuery<ApiDataSource>({
    queryKey: dataSourceKeys.detail(org, dataSourceId ?? -1),
    queryFn: () => dataSourceApi.getDataSource(org, dataSourceId!),
    enabled: dataSourceId !== undefined && dataSourceId > 0,
  });
};

// Imperative variant for renderDetail callers feeding React 19's `use()`.
// Reads from cache when fresh, falls back to network otherwise.
export const useFetchDataSource = () => {
  const { dataSourceApi, org } = useDataSourcesApi();
  const queryClient = useQueryClient();
  return (dataSourceId: number) =>
    queryClient.fetchQuery<ApiDataSource>({
      queryKey: dataSourceKeys.detail(org, dataSourceId),
      queryFn: () => dataSourceApi.getDataSource(org, dataSourceId),
    });
};

export const useSaveDataSourceMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiDataSource>, "mutationFn">,
) => {
  const { dataSourceApi, org } = useDataSourcesApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (dataSource: ApiDataSource) => {
      const dataSourceId =
        dataSource.dataSourceId && dataSource.dataSourceId > 0
          ? dataSource.dataSourceId
          : undefined;
      return dataSourceApi.postDatasource(org, { ...dataSource, dataSourceId });
    },
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: dataSourceKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};

export const useDeleteDataSourceMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { dataSourceApi, org } = useDataSourcesApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (dataSourceId: number) =>
      dataSourceApi.deleteDatasource(org, dataSourceId),
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: dataSourceKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};
