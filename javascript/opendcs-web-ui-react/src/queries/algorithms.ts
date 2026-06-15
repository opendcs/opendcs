import { useMemo } from "react";
import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from "@tanstack/react-query";
import {
  RESTAlgorithmMethodsApi,
  RESTRetrievingPropertySpecsApi,
  type ApiAlgorithm,
  type ApiAlgorithmRef,
  type ApiPropSpec,
  type ClassName,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { algorithmKeys } from "./keys";
import { invalidateThenDelegate, normalizeNewId } from "./mutationHelpers";

const useAlgorithmsApi = () => {
  const api = useApi();
  const algorithmApi = useMemo(() => new RESTAlgorithmMethodsApi(api.conf), [api.conf]);
  const propSpecsApi = useMemo(
    () => new RESTRetrievingPropertySpecsApi(api.conf),
    [api.conf],
  );
  return { algorithmApi, propSpecsApi, org: api.org };
};

export const useAlgorithmsQuery = () => {
  const { algorithmApi, org } = useAlgorithmsApi();
  return useQuery<ApiAlgorithmRef[]>({
    queryKey: algorithmKeys.list(org),
    queryFn: () => algorithmApi.getalgorithmrefs(org),
  });
};

export const useAlgorithmQuery = (algorithmId: number | undefined) => {
  const { algorithmApi, org } = useAlgorithmsApi();
  return useQuery<ApiAlgorithm>({
    queryKey: algorithmKeys.detail(org, algorithmId ?? -1),
    queryFn: () => algorithmApi.getalgorithm(org, algorithmId!),
    enabled: algorithmId !== undefined && algorithmId > 0,
  });
};

// Imperative variant for AlgorithmsTable.renderDetail, which feeds React 19's
// `use()`. Reads from cache when fresh, falls back to network otherwise.
export const useFetchAlgorithm = () => {
  const { algorithmApi, org } = useAlgorithmsApi();
  const queryClient = useQueryClient();
  return (algorithmId: number) =>
    queryClient.fetchQuery<ApiAlgorithm>({
      queryKey: algorithmKeys.detail(org, algorithmId),
      queryFn: () => algorithmApi.getalgorithm(org, algorithmId),
    });
};

// PropSpecs failures are non-fatal — the editor still works without them, so
// swallow the error and return [] (matches the prior console.warn behavior).
export const useFetchPropSpecs = () => {
  const { propSpecsApi, org } = useAlgorithmsApi();
  const queryClient = useQueryClient();
  return (execClass: string) =>
    queryClient
      .fetchQuery<ApiPropSpec[]>({
        queryKey: algorithmKeys.propSpecs(org, execClass),
        queryFn: () => propSpecsApi.getPropSpecs(org, execClass as ClassName),
      })
      .catch((e: unknown) => {
        console.warn("getPropSpecs failed for", execClass, e);
        return [] as ApiPropSpec[];
      });
};

export const useSaveAlgorithmMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, ApiAlgorithm>, "mutationFn">,
) => {
  const { algorithmApi, org } = useAlgorithmsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (algorithm: ApiAlgorithm) =>
      algorithmApi.postAlgorithm(org, {
        ...algorithm,
        algorithmId: normalizeNewId(algorithm.algorithmId),
      }),
    ...options,
    onSuccess: invalidateThenDelegate(
      queryClient,
      algorithmKeys.all(org),
      options?.onSuccess,
    ),
  });
};

export const useDeleteAlgorithmMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, number>, "mutationFn">,
) => {
  const { algorithmApi, org } = useAlgorithmsApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (algorithmId: number) => algorithmApi.deleteAlgorithm(org, algorithmId),
    ...options,
    onSuccess: invalidateThenDelegate(
      queryClient,
      algorithmKeys.all(org),
      options?.onSuccess,
    ),
  });
};

export interface CatalogAlgorithm {
  name: string;
  execClass: string;
  description: string;
  alreadyImported: boolean;
}

export const useAlgorithmCatalogQuery = (enabled: boolean) => {
  const { org } = useApi();
  return useQuery<CatalogAlgorithm[]>({
    queryKey: algorithmKeys.catalog(org),
    queryFn: () =>
      fetch("/odcsapi/algorithmcatalog", {
        headers: { "X-ORGANIZATION-ID": org },
      }).then((res) => {
        if (!res.ok) throw new Error(`Catalog fetch failed: ${res.status}`);
        return res.json() as Promise<CatalogAlgorithm[]>;
      }),
    enabled,
    staleTime: 5 * 60_000,
  });
};

export const useImportAlgorithmsMutation = (
  options?: Omit<UseMutationOptions<unknown, unknown, string[]>, "mutationFn">,
) => {
  const { org } = useApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (execClasses: string[]) =>
      fetch("/odcsapi/algorithmcatalog", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-ORGANIZATION-ID": org,
        },
        body: JSON.stringify(execClasses),
      }).then((res) => {
        if (!res.ok) throw new Error(`Import failed: ${res.status}`);
      }),
    ...options,
    // Invalidate the entity root (catalog + list) so the table reflects newly
    // imported records and updated alreadyImported flags.
    onSuccess: invalidateThenDelegate(
      queryClient,
      algorithmKeys.all(org),
      options?.onSuccess,
    ),
  });
};
