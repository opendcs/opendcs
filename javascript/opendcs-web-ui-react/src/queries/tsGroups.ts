import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  TimeSeriesMethodsGroupsApi,
  type ApiTimeSeriesIdentifier,
  type ApiTsGroup,
  type ApiTsGroupRef,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { tsGroupKeys } from "./keys";
import { invalidateThenDelegate, normalizeNewId } from "./mutationHelpers";

const useTsGroupsApi = () => {
  const api = useApi();
  const groupApi = useMemo(() => new TimeSeriesMethodsGroupsApi(api.conf), [api.conf]);
  return { groupApi, org: api.org };
};

// TS group refs power both the Time Series Groups list and the "group"
// dropdown on the Computations editor.
export const useTsGroupRefsQuery = () => {
  const { groupApi, org } = useTsGroupsApi();
  return useQuery<ApiTsGroupRef[]>({
    queryKey: tsGroupKeys.list(org),
    queryFn: () => groupApi.getTsGroupRefs(org),
  });
};

export const useTsGroupQuery = (groupId: number | undefined) => {
  const { groupApi, org } = useTsGroupsApi();
  return useQuery<ApiTsGroup>({
    queryKey: tsGroupKeys.detail(org, groupId ?? -1),
    queryFn: () => groupApi.getTsGroup(org, groupId!),
    enabled: groupId !== undefined && groupId > 0,
  });
};

// Imperative variant for renderDetail callers feeding React 19's `use()`.
// Reads from cache when fresh, falls back to network otherwise.
export const useFetchTsGroup = () => {
  const { groupApi, org } = useTsGroupsApi();
  const queryClient = useQueryClient();
  return (groupId: number) =>
    queryClient.fetchQuery<ApiTsGroup>({
      queryKey: tsGroupKeys.detail(org, groupId),
      queryFn: () => groupApi.getTsGroup(org, groupId),
    });
};

/**
 * Expanded membership of a saved group — the desktop editor's "Evaluate"
 * button. Only meaningful for a group that already exists server-side, so it
 * stays disabled for unsaved (non-positive id) groups.
 */
export const useExpandTsGroupQuery = (groupId: number | undefined, enabled = true) => {
  const { groupApi, org } = useTsGroupsApi();
  return useQuery<ApiTimeSeriesIdentifier[]>({
    queryKey: tsGroupKeys.expand(org, groupId ?? -1),
    queryFn: () => groupApi.expandGroup(org, groupId!),
    enabled: enabled && groupId !== undefined && groupId > 0,
    retry: 0,
  });
};

export const useSaveTsGroupMutation = (
  options?: Omit<UseMutationOptions<ApiTsGroup, unknown, ApiTsGroup>, "mutationFn">,
) => {
  const { groupApi, org } = useTsGroupsApi();
  const queryClient = useQueryClient();
  const invalidateList = invalidateThenDelegate<ApiTsGroup, unknown, ApiTsGroup>(
    queryClient,
    tsGroupKeys.all(org),
    options?.onSuccess,
  );
  return useMutation({
    mutationFn: (group: ApiTsGroup) =>
      groupApi.postTsGroup(org, { ...group, groupId: normalizeNewId(group.groupId) }),
    ...options,
    onSuccess: (...args) => {
      // The detail and the expansion both describe the pre-save definition;
      // drop them outright so the next open/evaluate refetches rather than
      // rendering a stale copy.
      const groupId = args[1].groupId;
      if (groupId != null && groupId > 0) {
        queryClient.removeQueries({ queryKey: tsGroupKeys.detail(org, groupId) });
        queryClient.removeQueries({ queryKey: tsGroupKeys.expand(org, groupId) });
      }
      invalidateList(...args);
    },
  });
};

export const useDeleteTsGroupMutation = (
  options?: Omit<UseMutationOptions<void, unknown, number>, "mutationFn">,
) => {
  const { groupApi, org } = useTsGroupsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupId: number) => groupApi.deleteTsGroup(org, groupId),
    ...options,
    onSuccess: invalidateThenDelegate<void, unknown, number>(
      queryClient,
      tsGroupKeys.all(org),
      options?.onSuccess,
    ),
  });
};
