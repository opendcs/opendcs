import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  RESTDECODESPresentationGroupRecordsApi,
  type ApiPresentationRef,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { presentationKeys } from "./keys";

const usePresentationsApi = () => {
  const api = useApi();
  const presentationApi = useMemo(
    () => new RESTDECODESPresentationGroupRecordsApi(api.conf),
    [api.conf],
  );
  return { presentationApi, org: api.org };
};

export const usePresentationRefsQuery = () => {
  const { presentationApi, org } = usePresentationsApi();
  return useQuery<ApiPresentationRef[]>({
    queryKey: presentationKeys.list(org),
    queryFn: () => presentationApi.getpresentationrefs(org),
  });
};
