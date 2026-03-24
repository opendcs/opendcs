import {
  type ApiAlgorithm,
  type ApiComputation,
  type ApiComputationRef,
  RESTAlgorithmMethodsApi,
  RESTComputationMethodsApi,
} from "opendcs-api";
import { ComputationsTable } from "./ComputationsTable";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useApi } from "../../../contexts/app/ApiContext";

export const Computations: React.FC = () => {
  const [computations, setComputations] = useState<ApiComputationRef[]>([]);
  const [stale, setStale] = useState(true);
  const api = useApi();
  const computationApi = useMemo(
    () => new RESTComputationMethodsApi(api.conf),
    [api.conf],
  );
  const algorithmApi = useMemo(() => new RESTAlgorithmMethodsApi(api.conf), [api.conf]);

  useEffect(() => {
    const fetchComputations = async () => {
      const refs = await computationApi.getComputationRefs(api.org);
      setComputations(refs);
      setStale(false);
    };
    if (stale === true) {
      fetchComputations();
    }
  }, [stale, api.org, computationApi]);

  const getComputation = useCallback(
    (computationId: number) => computationApi.getComputation(api.org, computationId),
    [api.org, computationApi],
  );

  const getAlgorithm = useCallback(
    (algorithmId: number): Promise<ApiAlgorithm> =>
      algorithmApi.getalgorithm(api.org, algorithmId),
    [api.org, algorithmApi],
  );

  const saveComputation = useCallback(
    (computation: ApiComputation) => {
      const computationId =
        computation.computationId && computation.computationId > 0
          ? computation.computationId
          : undefined;
      computationApi
        .postComputation(api.org, { ...computation, computationId })
        .then(() => setStale(true))
        .catch((e: unknown) => console.error("Failed to save computation", e));
    },
    [api.org, computationApi],
  );

  const deleteComputation = useCallback(
    (computationId: number) => {
      computationApi
        .deleteComputation(api.org, computationId)
        .then(() => setStale(true))
        .catch((e: unknown) => console.error("Failed to delete computation", e));
    },
    [api.org, computationApi],
  );

  return (
    <div className="content">
      <ComputationsTable
        computations={computations}
        getComputation={getComputation}
        getAlgorithm={getAlgorithm}
        actions={{ save: saveComputation, remove: deleteComputation }}
      />
    </div>
  );
};
