import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { TimeSeriesMethodsApi, type ApiTimeSeriesIdentifier } from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { timeSeriesKeys } from "./keys";

const useTimeSeriesApi = () => {
  const api = useApi();
  const tsApi = useMemo(() => new TimeSeriesMethodsApi(api.conf), [api.conf]);
  return { tsApi, org: api.org };
};

/**
 * Catalog of time series identifiers, used by pickers such as the Time Series
 * Group members chooser. The catalog can be large and changes rarely within a
 * session, so it is cached well past the default staleTime.
 */
export const useTimeSeriesRefsQuery = (active = true, enabled = true) => {
  const { tsApi, org } = useTimeSeriesApi();
  return useQuery<ApiTimeSeriesIdentifier[]>({
    queryKey: timeSeriesKeys.refs(org, active),
    queryFn: () => tsApi.getTimeSeriesRefs(org, active),
    staleTime: 10 * 60_000,
    enabled,
  });
};
