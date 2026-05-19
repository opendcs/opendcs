import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { TimeSeriesMethodsGroupsApi, type ApiTsGroupRef } from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { tsGroupKeys } from "./keys";

// TS group refs power the "group" dropdown on the Computations editor.
export const useTsGroupRefsQuery = () => {
  const api = useApi();
  const groupApi = useMemo(() => new TimeSeriesMethodsGroupsApi(api.conf), [api.conf]);
  return useQuery<ApiTsGroupRef[]>({
    queryKey: tsGroupKeys.list(api.org),
    queryFn: () => groupApi.getTsGroupRefs(api.org),
  });
};
