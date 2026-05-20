import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { TimeSeriesMethodsIntervalMethodsApi, type ApiInterval } from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { intervalKeys } from "./keys";

const useIntervalsApi = () => {
  const api = useApi();
  const intervalApi = useMemo(
    () => new TimeSeriesMethodsIntervalMethodsApi(api.conf),
    [api.conf],
  );
  return { intervalApi, org: api.org };
};

// Intervals are reference data — stable enough to cache for the session.
export const useIntervalsQuery = () => {
  const { intervalApi, org } = useIntervalsApi();
  return useQuery<ApiInterval[]>({
    queryKey: intervalKeys.list(org),
    queryFn: () => intervalApi.getIntervals(org),
    staleTime: 60 * 60_000,
  });
};
