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
import { invalidateThenDelegate, normalizeNewId } from "./mutationHelpers";

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
    mutationFn: (computation: ApiComputation) =>
      // Strip transient/derived ids when "new" so the server assigns them, and
      // clear server-managed timestamps so they're recomputed.
      computationApi.postComputation(org, {
        ...computation,
        computationId: normalizeNewId(computation.computationId),
        appId: normalizeNewId(computation.appId),
        groupId: normalizeNewId(computation.groupId),
        algorithmId: normalizeNewId(computation.algorithmId),
        lastModified: undefined,
        effectiveStartDate: undefined,
        effectiveEndDate: undefined,
      }),
    ...options,
    onSuccess: invalidateThenDelegate(
      queryClient,
      computationKeys.all(org),
      options?.onSuccess,
    ),
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
    onSuccess: invalidateThenDelegate(
      queryClient,
      computationKeys.all(org),
      options?.onSuccess,
    ),
  });
};
