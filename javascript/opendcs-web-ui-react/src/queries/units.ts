import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  RESTEngineeringUnitMethodsApi,
  type ApiUnit,
  type ApiUnitConverter,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { unitKeys } from "./keys";
import { REFERENCE_DATA_CACHE } from "./cachePolicy";

const useUnitsApi = () => {
  const api = useApi();
  const unitsApi = useMemo(
    () => new RESTEngineeringUnitMethodsApi(api.conf),
    [api.conf],
  );
  return { unitsApi, org: api.org };
};

export const useUnitListQuery = () => {
  const { unitsApi, org } = useUnitsApi();
  return useQuery<Record<number, ApiUnit>>({
    queryKey: unitKeys.list(org),
    queryFn: () => unitsApi.getUnitList(org),
    ...REFERENCE_DATA_CACHE,
  });
};

export const useUnitConversionsQuery = () => {
  const { unitsApi, org } = useUnitsApi();
  return useQuery<ApiUnitConverter[]>({
    queryKey: unitKeys.conversions(org),
    queryFn: () => unitsApi.getUnitConvList(org),
    ...REFERENCE_DATA_CACHE,
  });
};
