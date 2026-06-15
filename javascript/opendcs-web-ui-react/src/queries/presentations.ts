import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  RESTDECODESPresentationGroupRecordsApi,
  type ApiPresentationGroup,
  type ApiPresentationRef,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { presentationKeys } from "./keys";

const usePresentationsApi = () => {
  const api = useApi();
  const presentationApi = useMemo(
    () => new RESTDECODESPresentationGroupRecordsApi(api.conf),
    [api.conf],
  );
  return { presentationApi, org: api.org };
};

export const usePresentationRefsQuery = () => {
  const { presentationApi, org } = usePresentationsApi();
  return useQuery<ApiPresentationRef[]>({
    queryKey: presentationKeys.list(org),
    queryFn: () => presentationApi.getpresentationrefs(org),
  });
};

export const usePresentationQuery = (groupId: number | undefined) => {
  const { presentationApi, org } = usePresentationsApi();
  return useQuery<ApiPresentationGroup>({
    queryKey: presentationKeys.detail(org, groupId ?? -1),
    queryFn: () => presentationApi.getPresentation(org, groupId!),
    enabled: groupId !== undefined && groupId > 0,
  });
};

// Imperative variant for renderDetail callers feeding React 19's `use()`.
// Reads from cache when fresh, falls back to network otherwise.
export const useFetchPresentation = () => {
  const { presentationApi, org } = usePresentationsApi();
  const queryClient = useQueryClient();
  return (groupId: number) =>
    queryClient.fetchQuery<ApiPresentationGroup>({
      queryKey: presentationKeys.detail(org, groupId),
      queryFn: () => presentationApi.getPresentation(org, groupId),
    });
};

export const useSavePresentationMutation = (
  options?: Omit<
    UseMutationOptions<unknown, unknown, ApiPresentationGroup>,
    "mutationFn"
  >,
) => {
  const { presentationApi, org } = usePresentationsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (group: ApiPresentationGroup) => {
      const groupId = group.groupId && group.groupId > 0 ? group.groupId : undefined;
      return presentationApi.postPresentation(org, { ...group, groupId });
    },
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: presentationKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};

export const useDeletePresentationMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { presentationApi, org } = usePresentationsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupId: number) => presentationApi.deletePresentation(org, groupId),
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: presentationKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};
