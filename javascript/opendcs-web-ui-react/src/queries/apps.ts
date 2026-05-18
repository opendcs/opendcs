import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { RESTLoadingApplicationRecordsApi, type ApiAppRef } from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { appKeys } from "./keys";

// App refs power the "process" dropdown on the Computations editor. The list
// is small and rarely changes, so default staleTime is fine.
export const useAppRefsQuery = () => {
  const api = useApi();
  const appApi = useMemo(
    () => new RESTLoadingApplicationRecordsApi(api.conf),
    [api.conf],
  );
  return useQuery<ApiAppRef[]>({
    queryKey: appKeys.list(api.org),
    queryFn: () => appApi.getAppRefs(api.org),
  });
};
