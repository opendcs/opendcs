import {
  type ApiAlgorithm,
  type ApiAppRef,
  type ApiComputation,
  type ApiComputationRef,
  type ApiTsGroupRef,
  RESTAlgorithmMethodsApi,
  RESTComputationMethodsApi,
  RESTLoadingApplicationRecordsApi,
  TimeSeriesMethodsGroupsApi,
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
  const appApi = useMemo(
    () => new RESTLoadingApplicationRecordsApi(api.conf),
    [api.conf],
  );
  const groupApi = useMemo(() => new TimeSeriesMethodsGroupsApi(api.conf), [api.conf]);
  const [processOptions, setProcessOptions] = useState<ApiAppRef[]>([]);
  const [groupOptions, setGroupOptions] = useState<ApiTsGroupRef[]>([]);

  useEffect(() => {
    if (!api.org) return;
    let cancelled = false;
    appApi
      .getAppRefs(api.org)
      .then((refs) => {
        if (!cancelled) setProcessOptions(refs);
      })
      .catch(() => {
        if (!cancelled) setProcessOptions([]);
      });
    return () => {
      cancelled = true;
    };
  }, [api.org, appApi]);

  useEffect(() => {
    if (!api.org) return;
    let cancelled = false;
    groupApi
      .getTsGroupRefs(api.org)
      .then((refs) => {
        if (!cancelled) setGroupOptions(refs);
      })
      .catch(() => {
        if (!cancelled) setGroupOptions([]);
      });
    return () => {
      cancelled = true;
    };
  }, [api.org, groupApi]);

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
    (computation: ApiComputation): Promise<void> => {
      const computationId =
        computation.computationId && computation.computationId > 0
          ? computation.computationId
          : undefined;
      const appId =
        computation.appId && computation.appId > 0 ? computation.appId : undefined;
      const groupId =
        computation.groupId && computation.groupId > 0
          ? computation.groupId
          : undefined;
      const algorithmId =
        computation.algorithmId && computation.algorithmId > 0
          ? computation.algorithmId
          : undefined;
      return computationApi
        .postComputation(api.org, {
          ...computation,
          computationId,
          appId,
          groupId,
          algorithmId,
          lastModified: undefined,
          effectiveStartDate: undefined,
          effectiveEndDate: undefined,
        })
        .then(() => setStale(true));
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
        processOptions={processOptions}
        groupOptions={groupOptions}
      />
    </div>
  );
};
