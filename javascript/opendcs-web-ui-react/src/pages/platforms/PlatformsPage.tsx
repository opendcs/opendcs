import {
  type ApiPlatform,
  type ApiPlatformRef,
  RESTDECODESPlatformConfigurationsApi,
  RESTDECODESPlatformRecordsApi,
  RESTDECODESSiteRecordsApi,
} from "opendcs-api";
import { PlatformsTable } from "./PlatformsTable";
import { useCallback, useMemo } from "react";
import { useApi } from "../../contexts/app/ApiContext";
import { useStaleFetch } from "../../hooks/useStaleFetch";

export const PlatformsPage: React.FC = () => {
  const api = useApi();
  const platformApi = useMemo(
    () => new RESTDECODESPlatformRecordsApi(api.conf),
    [api.conf],
  );
  const siteApi = useMemo(() => new RESTDECODESSiteRecordsApi(api.conf), [api.conf]);
  const configApi = useMemo(
    () => new RESTDECODESPlatformConfigurationsApi(api.conf),
    [api.conf],
  );

  const fetchPlatformRefs = useCallback(
    () => platformApi.getPlatformRefs(api.org),
    [platformApi, api.org],
  );
  const {
    data: platforms,
    loading,
    refresh,
  } = useStaleFetch<ApiPlatformRef[]>(fetchPlatformRefs, []);

  const getPlatform = useCallback(
    (platformId: number) => platformApi.getPlatform(api.org, platformId),
    [api.org, platformApi],
  );

  const getSite = useCallback(
    (siteId: number) => siteApi.getsite(api.org, siteId),
    [api.org, siteApi],
  );

  const getConfig = useCallback(
    (configId: number) => configApi.getConfig(api.org, configId),
    [api.org, configApi],
  );

  const savePlatform = useCallback(
    (platform: ApiPlatform): Promise<void> => {
      const platformId =
        platform.platformId && platform.platformId > 0
          ? platform.platformId
          : undefined;
      return platformApi
        .postPlatform(api.org, { ...platform, platformId })
        .then(() => refresh())
        .catch((e: unknown) => {
          console.error("Failed to save platform", e);
        });
    },
    [api.org, platformApi, refresh],
  );

  const deletePlatform = useCallback(
    (platformId: number) => {
      platformApi
        .deletePlatform(api.org, platformId)
        .then(() => refresh())
        .catch((e: unknown) => console.error("Failed to delete platform", e));
    },
    [api.org, platformApi, refresh],
  );

  return (
    <div className="content">
      <PlatformsTable
        platforms={platforms}
        loading={loading}
        getPlatform={getPlatform}
        getSite={getSite}
        getConfig={getConfig}
        actions={{ save: savePlatform, remove: deletePlatform }}
      />
    </div>
  );
};
