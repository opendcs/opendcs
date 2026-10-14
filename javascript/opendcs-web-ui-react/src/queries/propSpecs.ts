import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  RESTRetrievingPropertySpecsApi,
  type ApiPropSpec,
  type ClassName,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { propSpecKeys } from "./keys";
import { REFERENCE_DATA_CACHE } from "./cachePolicy";

// The properties an OpenDCS class accepts. They come from the class itself, so
// they are cached like the other reference data. Disabled until a class is
// known. Not retried: a class the API cannot describe answers 409 every time,
// and an editor works without its specs.
export const usePropSpecsQuery = (execClass: string | undefined) => {
  const api = useApi();
  const propSpecsApi = useMemo(
    () => new RESTRetrievingPropertySpecsApi(api.conf),
    [api.conf],
  );
  return useQuery<ApiPropSpec[]>({
    queryKey: propSpecKeys.detail(api.org, execClass ?? ""),
    // The generated ClassName enum is narrower than what the endpoint accepts:
    // it takes any class name and reflects on it.
    queryFn: () => propSpecsApi.getPropSpecs(api.org, execClass as ClassName),
    enabled: !!execClass,
    retry: false,
    ...REFERENCE_DATA_CACHE,
  });
};
