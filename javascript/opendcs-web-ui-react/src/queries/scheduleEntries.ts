import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  RESTScheduleEntryMethodsApi,
  type ApiScheduleEntry,
  type ApiScheduleEntryRef,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { scheduleKeys } from "./keys";

const useScheduleApi = () => {
  const api = useApi();
  const scheduleApi = useMemo(
    () => new RESTScheduleEntryMethodsApi(api.conf),
    [api.conf],
  );
  return { scheduleApi, org: api.org };
};

export const useScheduleRefsQuery = () => {
  const { scheduleApi, org } = useScheduleApi();
  return useQuery<ApiScheduleEntryRef[]>({
    queryKey: scheduleKeys.list(org),
    queryFn: () => scheduleApi.getScheduleRefs(org),
  });
};

export const useScheduleQuery = (schedEntryId: number | undefined) => {
  const { scheduleApi, org } = useScheduleApi();
  return useQuery<ApiScheduleEntry>({
    queryKey: scheduleKeys.detail(org, schedEntryId ?? -1),
    queryFn: () => scheduleApi.getSchedule(org, schedEntryId!),
    enabled: schedEntryId !== undefined && schedEntryId > 0,
  });
};

// Imperative variant for renderDetail callers feeding React 19's `use()`.
// Reads from cache when fresh, falls back to network otherwise.
export const useFetchSchedule = () => {
  const { scheduleApi, org } = useScheduleApi();
  const queryClient = useQueryClient();
  return (schedEntryId: number) =>
    queryClient.fetchQuery<ApiScheduleEntry>({
      queryKey: scheduleKeys.detail(org, schedEntryId),
      queryFn: () => scheduleApi.getSchedule(org, schedEntryId),
    });
};

export const useSaveScheduleMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiScheduleEntry>, "mutationFn">,
) => {
  const { scheduleApi, org } = useScheduleApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (entry: ApiScheduleEntry) => {
      const schedEntryId =
        entry.schedEntryId && entry.schedEntryId > 0 ? entry.schedEntryId : undefined;
      return scheduleApi.postSchedule(org, { ...entry, schedEntryId });
    },
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: scheduleKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};

export const useDeleteScheduleMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { scheduleApi, org } = useScheduleApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (schedEntryId: number) => scheduleApi.deleteSchedule(org, schedEntryId),
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: scheduleKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};
