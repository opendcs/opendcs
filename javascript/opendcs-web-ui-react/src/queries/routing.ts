import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  RESTDECODESRoutingSpecRecordsApi,
  type ApiRouting,
  type ApiRoutingRef,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { routingKeys } from "./keys";

const useRoutingApi = () => {
  const api = useApi();
  const routingApi = useMemo(
    () => new RESTDECODESRoutingSpecRecordsApi(api.conf),
    [api.conf],
  );
  return { routingApi, org: api.org };
};

export const useRoutingsQuery = () => {
  const { routingApi, org } = useRoutingApi();
  return useQuery<ApiRoutingRef[]>({
    queryKey: routingKeys.list(org),
    queryFn: () => routingApi.getRoutingRefs(org),
  });
};

export const useRoutingQuery = (routingId: number | undefined) => {
  const { routingApi, org } = useRoutingApi();
  return useQuery<ApiRouting>({
    queryKey: routingKeys.detail(org, routingId ?? -1),
    queryFn: () => routingApi.getRouting(org, routingId!),
    enabled: routingId !== undefined && routingId > 0,
  });
};

// Imperative variant for renderDetail callers feeding React 19's `use()`.
// Reads from cache when fresh, falls back to network otherwise.
export const useFetchRouting = () => {
  const { routingApi, org } = useRoutingApi();
  const queryClient = useQueryClient();
  return (routingId: number) =>
    queryClient.fetchQuery<ApiRouting>({
      queryKey: routingKeys.detail(org, routingId),
      queryFn: () => routingApi.getRouting(org, routingId),
    });
};

export const useSaveRoutingMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiRouting>, "mutationFn">,
) => {
  const { routingApi, org } = useRoutingApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (routing: ApiRouting) => {
      const routingId =
        routing.routingId && routing.routingId > 0 ? routing.routingId : undefined;
      return routingApi.postRouting(org, { ...routing, routingId });
    },
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: routingKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};

export const useDeleteRoutingMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { routingApi, org } = useRoutingApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (routingId: number) => routingApi.deleteRouting(org, routingId),
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: routingKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};
