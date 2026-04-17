import {
  type ApiAlgorithm,
  type ApiAlgorithmRef,
  type ApiPropSpec,
  type ClassName,
  RESTAlgorithmMethodsApi,
  RESTRetrievingPropertySpecsApi,
} from "opendcs-api";
import { AlgorithmsTable } from "./AlgorithmsTable";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useApi } from "../../../contexts/app/ApiContext";

export const Algorithms: React.FC = () => {
  const [algorithms, setAlgorithms] = useState<ApiAlgorithmRef[]>([]);
  const [stale, setStale] = useState(true);
  const api = useApi();
  const algorithmApi = useMemo(() => new RESTAlgorithmMethodsApi(api.conf), [api.conf]);
  const propSpecsApi = useMemo(
    () => new RESTRetrievingPropertySpecsApi(api.conf),
    [api.conf],
  );

  useEffect(() => {
    let cancelled = false;
    if (stale) {
      algorithmApi.getalgorithmrefs(api.org).then((refs) => {
        if (!cancelled) {
          setAlgorithms(refs);
          setStale(false);
        }
      });
    }
    return () => {
      cancelled = true;
    };
  }, [stale, api.org, algorithmApi]);

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
    (algorithm: ApiAlgorithm) => {
      const algorithmId =
        algorithm.algorithmId && algorithm.algorithmId > 0
          ? algorithm.algorithmId
          : undefined;
      algorithmApi
        .postAlgorithm(api.org, { ...algorithm, algorithmId })
        .then(() => setStale(true))
        .catch((e: unknown) => console.error("Failed to save algorithm", e));
    },
    [api.org, algorithmApi],
  );

  const deleteAlgorithm = useCallback(
    (algorithmId: number) => {
      algorithmApi
        .deleteAlgorithm(api.org, algorithmId)
        .then(() => setStale(true))
        .catch((e: unknown) => console.error("Failed to delete algorithm", e));
    },
    [api.org, algorithmApi],
  );

  return (
    <div className="content">
      <AlgorithmsTable
        algorithms={algorithms}
        loading={stale}
        getAlgorithm={getAlgorithm}
        getPropSpecs={getPropSpecs}
        actions={{ save: saveAlgorithm, remove: deleteAlgorithm }}
        onRefresh={() => setStale(true)}
      />
    </div>
  );
};
