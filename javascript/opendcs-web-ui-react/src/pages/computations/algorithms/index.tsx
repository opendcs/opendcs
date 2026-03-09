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
    const fetchAlgorithms = async () => {
      const refs = await algorithmApi.getalgorithmrefs(api.org);
      setAlgorithms(refs);
      setStale(false);
    };
    if (stale === true) {
      fetchAlgorithms();
    }
  }, [stale, api.org, algorithmApi]);

  const getAlgorithm = useCallback(
    async (algorithmId: number) => {
      const result = await algorithmApi.getalgorithm(api.org, algorithmId);
      // ObjectSerializer destroys the parms array (types it as single object).
      // Fetch raw JSON alongside and attach the real parms array.
      try {
        const raw = await fetch(`/odcsapi/algorithm?algorithmid=${algorithmId}`, {
          headers: { "X-ORGANIZATION-ID": api.org || "" },
        });
        if (raw.ok) {
          const json = (await raw.json()) as Record<string, unknown>;
          (result as Record<string, unknown>)["parms"] = json["parms"] ?? [];
        }
      } catch (e: unknown) {
        console.warn("Failed to fetch raw algorithm parms", e);
      }
      return result;
    },
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
      // parms is attached as AlgoParm[] via cast in Algorithm.tsx — extract before stripping.
      // algoScripts: TS client types it as single object but server expects array — strip it.
      const parms = (algorithm as unknown as Record<string, unknown>)["parms"] ?? [];
      const { parms: _parms, algoScripts: _algoScripts, ...rest } = algorithm;
      const algorithmId =
        rest.algorithmId && rest.algorithmId > 0 ? rest.algorithmId : undefined;
      // Use raw fetch so parms array is serialized correctly (ObjectSerializer would destroy it).
      fetch("/odcsapi/algorithm", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-ORGANIZATION-ID": api.org || "",
        },
        body: JSON.stringify({ ...rest, algorithmId, parms }),
      })
        .then((r) => {
          if (!r.ok) throw new Error(`HTTP ${r.status}`);
          setStale(true);
        })
        .catch((e: unknown) => console.error("Failed to save algorithm", e));
    },
    [api.org],
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
        getAlgorithm={getAlgorithm}
        getPropSpecs={getPropSpecs}
        actions={{ save: saveAlgorithm, remove: deleteAlgorithm }}
      />
    </div>
  );
};
