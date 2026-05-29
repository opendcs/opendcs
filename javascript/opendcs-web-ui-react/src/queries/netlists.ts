import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import { RESTNetworkListsApi, type ApiNetList, type ApiNetlistRef } from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { netlistKeys } from "./keys";

const useNetlistsApi = () => {
  const api = useApi();
  const netlistApi = useMemo(() => new RESTNetworkListsApi(api.conf), [api.conf]);
  return { netlistApi, org: api.org };
};

export const useNetlistRefsQuery = () => {
  const { netlistApi, org } = useNetlistsApi();
  return useQuery<ApiNetlistRef[]>({
    queryKey: netlistKeys.list(org),
    queryFn: () => netlistApi.getNetlistRefs(org),
  });
};

export const useNetlistQuery = (netlistId: number | undefined) => {
  const { netlistApi, org } = useNetlistsApi();
  return useQuery<ApiNetList>({
    queryKey: netlistKeys.detail(org, netlistId ?? -1),
    queryFn: () => netlistApi.getNetList(org, netlistId!),
    enabled: netlistId !== undefined && netlistId > 0,
  });
};

// Imperative variant for renderDetail callers feeding React 19's `use()`.
// Reads from cache when fresh, falls back to network otherwise.
export const useFetchNetlist = () => {
  const { netlistApi, org } = useNetlistsApi();
  const queryClient = useQueryClient();
  return (netlistId: number) =>
    queryClient.fetchQuery<ApiNetList>({
      queryKey: netlistKeys.detail(org, netlistId),
      queryFn: () => netlistApi.getNetList(org, netlistId),
    });
};

export const useSaveNetlistMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiNetList>, "mutationFn">,
) => {
  const { netlistApi, org } = useNetlistsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (netlist: ApiNetList) => {
      const netlistId =
        netlist.netlistId && netlist.netlistId > 0 ? netlist.netlistId : undefined;
      return netlistApi.postNetlist(org, { ...netlist, netlistId });
    },
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: netlistKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};

export const useDeleteNetlistMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { netlistApi, org } = useNetlistsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (netlistId: number) => netlistApi.deleteNetlist(org, netlistId),
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: netlistKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};
