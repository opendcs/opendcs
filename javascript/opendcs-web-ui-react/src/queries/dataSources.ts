import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { RESTDECODESDataSourceRecordsApi, type ApiDataSourceRef } from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { dataSourceKeys } from "./keys";

const useDataSourcesApi = () => {
  const api = useApi();
  const dataSourceApi = useMemo(
    () => new RESTDECODESDataSourceRecordsApi(api.conf),
    [api.conf],
  );
  return { dataSourceApi, org: api.org };
};

export const useDataSourceRefsQuery = () => {
  const { dataSourceApi, org } = useDataSourcesApi();
  return useQuery<ApiDataSourceRef[]>({
    queryKey: dataSourceKeys.list(org),
    queryFn: () => dataSourceApi.getDataSourceRefs(org),
  });
};
