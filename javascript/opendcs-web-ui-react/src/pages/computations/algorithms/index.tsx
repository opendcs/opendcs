import {
  type ApiAlgorithm,
  type ApiAlgorithmRef,
  type ApiPropSpec,
  type ClassName,
  RESTAlgorithmMethodsApi,
  RESTRetrievingPropertySpecsApi,
} from "opendcs-api";
import { AlgorithmsTable } from "./AlgorithmsTable";
import { useCallback, useMemo } from "react";
import { useApi } from "../../../contexts/app/ApiContext";
import { useStaleFetch } from "../../../hooks/useStaleFetch";

export const Algorithms: React.FC = () => {
  const api = useApi();
  const algorithmApi = useMemo(() => new RESTAlgorithmMethodsApi(api.conf), [api.conf]);
  const propSpecsApi = useMemo(
    () => new RESTRetrievingPropertySpecsApi(api.conf),
    [api.conf],
  );

  const fetchAlgorithmRefs = useCallback(
    () => algorithmApi.getalgorithmrefs(api.org),
    [algorithmApi, api.org],
  );
  const {
    data: algorithms,
    loading,
    refresh,
  } = useStaleFetch<ApiAlgorithmRef[]>(fetchAlgorithmRefs, []);

  const getAlgorithm = useCallback(
    (algorithmId: number) => algorithmApi.getalgorithm(api.org, algorithmId),
    [api.org, algorithmApi],
  );

  const getPropSpecs = useCallback(
    (execClass: string): Promise<ApiPropSpec[]> => {
      return propSpecsApi
        .getPropSpecs(api.org, execClass as ClassName)
        .catch((e: unknown) => {
          console.warn("getPropSpecs failed for", execClass, e);
          return [];
        });
    },
    [api.org, propSpecsApi],
  );

  const saveAlgorithm = useCallback(
    (algorithm: ApiAlgorithm): Promise<void> => {
      const algorithmId =
        algorithm.algorithmId && algorithm.algorithmId > 0
          ? algorithm.algorithmId
          : undefined;
      // Return the POST promise so the table wrapper can await it before
      // transitioning the detail back to show mode (avoids save→refetch race).
      return algorithmApi
        .postAlgorithm(api.org, { ...algorithm, algorithmId })
        .then(() => refresh())
        .catch((e: unknown) => {
          console.error("Failed to save algorithm", e);
        });
    },
    [api.org, algorithmApi, refresh],
  );

  const deleteAlgorithm = useCallback(
    (algorithmId: number) => {
      algorithmApi
        .deleteAlgorithm(api.org, algorithmId)
        .then(() => refresh())
        .catch((e: unknown) => console.error("Failed to delete algorithm", e));
    },
    [api.org, algorithmApi, refresh],
  );

  return (
    <div className="content">
      <AlgorithmsTable
        algorithms={algorithms}
        loading={loading}
        getAlgorithm={getAlgorithm}
        getPropSpecs={getPropSpecs}
        actions={{ save: saveAlgorithm, remove: deleteAlgorithm }}
        onRefresh={refresh}
      />
    </div>
  );
};
