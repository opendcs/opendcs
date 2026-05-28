import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { RESTNetworkListsApi, type ApiNetlistRef } from "opendcs-api";
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
