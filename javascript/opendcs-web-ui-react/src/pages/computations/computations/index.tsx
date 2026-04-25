import {
  type ApiAlgorithm,
  type ApiAppRef,
  type ApiComputation,
  type ApiComputationRef,
  type ApiDataType,
  type ApiSiteRef,
  type ApiTsGroupRef,
  RESTAlgorithmMethodsApi,
  RESTComputationMethodsApi,
  RESTDECODESSiteRecordsApi,
  RESTDataTypeMethodsApi,
  RESTLoadingApplicationRecordsApi,
  TimeSeriesMethodsApi,
  TimeSeriesMethodsGroupsApi,
} from "opendcs-api";
import { ComputationsTable } from "./ComputationsTable";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useApi } from "../../../contexts/app/ApiContext";
import {
  loadRunResultTimeSeries,
  runComputationStream,
  type ComputationRunResult,
} from "./computationRun";
import { resolveComputationParmReferences } from "./computationSave";

const toComputationRef = (saved: ApiComputation): ApiComputationRef => ({
  ...saved,
  processId: saved.appId,
  processName: saved.applicationName,
  description: saved.comment,
});

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
  const timeSeriesApi = useMemo(() => new TimeSeriesMethodsApi(api.conf), [api.conf]);
  const siteApi = useMemo(() => new RESTDECODESSiteRecordsApi(api.conf), [api.conf]);
  const dataTypeApi = useMemo(() => new RESTDataTypeMethodsApi(api.conf), [api.conf]);
  const [processOptions, setProcessOptions] = useState<ApiAppRef[]>([]);
  const [groupOptions, setGroupOptions] = useState<ApiTsGroupRef[]>([]);
  const [siteOptions, setSiteOptions] = useState<ApiSiteRef[]>([]);
  const [dataTypeOptions, setDataTypeOptions] = useState<ApiDataType[]>([]);

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
    if (!api.org) return;
    let cancelled = false;
    siteApi
      .getsiterefs(api.org)
      .then((refs) => {
        if (!cancelled) setSiteOptions(refs);
      })
      .catch(() => {
        if (!cancelled) setSiteOptions([]);
      });
    return () => {
      cancelled = true;
    };
  }, [api.org, siteApi]);

  useEffect(() => {
    if (!api.org) return;
    let cancelled = false;
    dataTypeApi
      .getDataTypeList(api.org)
      .then((items) => {
        if (!cancelled) setDataTypeOptions(items);
      })
      .catch(() => {
        if (!cancelled) setDataTypeOptions([]);
      });
    return () => {
      cancelled = true;
    };
  }, [api.org, dataTypeApi]);

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
    async (computation: ApiComputation) => {
      const computationId =
        computation.computationId && computation.computationId > 0
          ? computation.computationId
          : undefined;
      try {
        const [resolvedSites, resolvedDataTypes] = await Promise.all([
          siteOptions.length > 0
            ? Promise.resolve(siteOptions)
            : siteApi.getsiterefs(api.org).catch(() => [] as ApiSiteRef[]),
          dataTypeOptions.length > 0
            ? Promise.resolve(dataTypeOptions)
            : dataTypeApi.getDataTypeList(api.org).catch(() => [] as ApiDataType[]),
        ]);
        if (siteOptions.length === 0 && resolvedSites.length > 0) {
          setSiteOptions(resolvedSites);
        }
        if (dataTypeOptions.length === 0 && resolvedDataTypes.length > 0) {
          setDataTypeOptions(resolvedDataTypes);
        }
        const saved = await computationApi.postComputation(api.org, {
          ...resolveComputationParmReferences(
            computation,
            resolvedSites,
            resolvedDataTypes,
          ),
          computationId,
        });
        if (saved.computationId && saved.computationId > 0) {
          setComputations((current) => {
            const savedRef = toComputationRef(saved);
            const existingIndex = current.findIndex(
              (item) => item.computationId === saved.computationId,
            );
            if (existingIndex < 0) {
              return [...current, savedRef];
            }
            return current.map((item, index) =>
              index === existingIndex ? savedRef : item,
            );
          });
        }
        setStale(true);
        return saved;
      } catch (e: unknown) {
        console.error("Failed to save computation", e);
        throw e;
      }
    },
    [api.org, computationApi, siteOptions, dataTypeOptions, siteApi, dataTypeApi],
  );

  const deleteComputation = useCallback(
    async (computationId: number) => {
      try {
        await computationApi.deleteComputation(api.org, computationId);
        setStale(true);
      } catch (e: unknown) {
        console.error("Failed to delete computation", e);
        throw e;
      }
    },
    [api.org, computationApi],
  );

  const runComputation = useCallback(
    async (
      computationId: number,
      start: Date,
      end: Date,
    ): Promise<ComputationRunResult> => {
      const runResult = await runComputationStream(api.org, computationId, start, end);
      return loadRunResultTimeSeries(timeSeriesApi, api.org, runResult);
    },
    [api.org, timeSeriesApi],
  );

  return (
    <div className="content">
      <ComputationsTable
        computations={computations}
        getComputation={getComputation}
        getAlgorithm={getAlgorithm}
        actions={{
          save: saveComputation,
          remove: deleteComputation,
          run: runComputation,
        }}
        processOptions={processOptions}
        groupOptions={groupOptions}
      />
    </div>
  );
};
