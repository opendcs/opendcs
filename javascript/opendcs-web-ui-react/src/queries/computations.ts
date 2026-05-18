import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  RESTComputationMethodsApi,
  type ApiComputation,
  type ApiComputationRef,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { computationKeys } from "./keys";

const useComputationsApi = () => {
  const api = useApi();
  const computationApi = useMemo(
    () => new RESTComputationMethodsApi(api.conf),
    [api.conf],
  );
  return { computationApi, org: api.org };
};

export const useComputationsQuery = () => {
  const { computationApi, org } = useComputationsApi();
  return useQuery<ApiComputationRef[]>({
    queryKey: computationKeys.list(org),
    queryFn: () => computationApi.getComputationRefs(org),
  });
};

export const useFetchComputation = () => {
  const { computationApi, org } = useComputationsApi();
  const queryClient = useQueryClient();
  return (computationId: number) =>
    queryClient.fetchQuery<ApiComputation>({
      queryKey: computationKeys.detail(org, computationId),
      queryFn: () => computationApi.getComputation(org, computationId),
    });
};

export const useSaveComputationMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiComputation>, "mutationFn">,
) => {
  const { computationApi, org } = useComputationsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (computation: ApiComputation) => {
      // Strip transient/derived ids when "new" so the server assigns them, and
      // clear server-managed timestamps so they're recomputed.
      const computationId =
        computation.computationId && computation.computationId > 0
          ? computation.computationId
          : undefined;
      const appId =
        computation.appId && computation.appId > 0 ? computation.appId : undefined;
      const groupId =
        computation.groupId && computation.groupId > 0
          ? computation.groupId
          : undefined;
      const algorithmId =
        computation.algorithmId && computation.algorithmId > 0
          ? computation.algorithmId
          : undefined;
      return computationApi.postComputation(org, {
        ...computation,
        computationId,
        appId,
        groupId,
        algorithmId,
        lastModified: undefined,
        effectiveStartDate: undefined,
        effectiveEndDate: undefined,
      });
    },
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: computationKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};

export const useDeleteComputationMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { computationApi, org } = useComputationsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (computationId: number) =>
      computationApi.deleteComputation(org, computationId),
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: computationKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};
