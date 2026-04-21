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
import { useNavigate } from "react-router-dom";
import {
  applySelectedAlgorithmToWorkspace,
  loadComputationWorkspace,
} from "../computations/computationWorkspace";

export const Algorithms: React.FC = () => {
  const [algorithms, setAlgorithms] = useState<ApiAlgorithmRef[] | null>(null);
  const [stale, setStale] = useState(true);
  const api = useApi();
  const navigate = useNavigate();
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
    async (algorithm: ApiAlgorithm) => {
      const algorithmId =
        algorithm.algorithmId && algorithm.algorithmId > 0
          ? algorithm.algorithmId
          : undefined;
      try {
        const saved = await algorithmApi.postAlgorithm(api.org, {
          ...algorithm,
          algorithmId,
        });
        setStale(true);
        return saved;
      } catch (e: unknown) {
        console.error("Failed to save algorithm", e);
        throw e;
      }
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

  if (algorithms === null) {
    return null;
  }

  return (
    <div className="content">
      <AlgorithmsTable
        algorithms={algorithms}
        getAlgorithm={getAlgorithm}
        getPropSpecs={getPropSpecs}
        actions={{ save: saveAlgorithm, remove: deleteAlgorithm }}
        onUseForComputation={async (algorithm) => {
          if (!algorithm.algorithmId) {
            return;
          }

          const fullAlgorithm = await getAlgorithm(algorithm.algorithmId).catch(
            (e: unknown) => {
              console.warn(
                "Failed to load full algorithm for computation selection",
                e,
              );
              return undefined;
            },
          );
          const propSpecs = fullAlgorithm?.execClass
            ? await getPropSpecs(fullAlgorithm.execClass)
            : [];

          applySelectedAlgorithmToWorkspace({
            algorithmId: algorithm.algorithmId,
            algorithmName: fullAlgorithm?.name ?? algorithm.algorithmName ?? "",
            algorithmDescription:
              fullAlgorithm?.description ?? algorithm.description ?? "",
            algorithmParms: fullAlgorithm?.parms ?? [],
            algorithmProps: fullAlgorithm?.props ?? {},
            algorithmPropSpecs: propSpecs,
          });
          navigate("/computations");
        }}
        computationSelectionActive={
          loadComputationWorkspace().selectionTargetId !== null
        }
        onRefresh={() => setStale(true)}
      />
    </div>
  );
};
