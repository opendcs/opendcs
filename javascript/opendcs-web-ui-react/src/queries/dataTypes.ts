import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { RESTDataTypeMethodsApi, type ApiDataType } from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { dataTypeKeys } from "./keys";

const useDataTypesApi = () => {
  const api = useApi();
  const dataTypesApi = useMemo(() => new RESTDataTypeMethodsApi(api.conf), [api.conf]);
  return { dataTypesApi, org: api.org };
};

export const useDataTypeListQuery = () => {
  const { dataTypesApi, org } = useDataTypesApi();
  return useQuery<ApiDataType[]>({
    queryKey: dataTypeKeys.list(org),
    queryFn: () => dataTypesApi.getDataTypeList(org),
    staleTime: 60 * 60_000,
  });
};
