import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  RESTDECODESEquipmentModelRecordsApi,
  type ApiEquipmentModel,
  type ApiEquipmentModelRef,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { equipmentKeys } from "./keys";

const useEquipmentApi = () => {
  const api = useApi();
  const equipmentApi = useMemo(
    () => new RESTDECODESEquipmentModelRecordsApi(api.conf),
    [api.conf],
  );
  return { equipmentApi, org: api.org };
};

export const useEquipmentRefsQuery = () => {
  const { equipmentApi, org } = useEquipmentApi();
  return useQuery<ApiEquipmentModelRef[]>({
    queryKey: equipmentKeys.list(org),
    queryFn: () => equipmentApi.getequipmentrefs(org),
  });
};

export const useFetchEquipment = () => {
  const { equipmentApi, org } = useEquipmentApi();
  const queryClient = useQueryClient();
  return (equipmentId: number) =>
    queryClient.fetchQuery<ApiEquipmentModel>({
      queryKey: equipmentKeys.detail(org, equipmentId),
      queryFn: () => equipmentApi.getequipment(org, equipmentId),
    });
};

export const useSaveEquipmentMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiEquipmentModel>, "mutationFn">,
) => {
  const { equipmentApi, org } = useEquipmentApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (equipment: ApiEquipmentModel) => {
      const equipmentId =
        equipment.equipmentId && equipment.equipmentId > 0
          ? equipment.equipmentId
          : undefined;
      return equipmentApi.postequipment(org, { ...equipment, equipmentId });
    },
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: equipmentKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};

export const useDeleteEquipmentMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { equipmentApi, org } = useEquipmentApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (equipmentId: number) => equipmentApi.deleteequipment(org, equipmentId),
    ...options,
    onSuccess: (...args) => {
      queryClient.invalidateQueries({ queryKey: equipmentKeys.all(org) });
      options?.onSuccess?.(...args);
    },
  });
};
