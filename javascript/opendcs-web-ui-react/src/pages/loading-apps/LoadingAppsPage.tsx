import {
  ApiLoadingApp,
  RESTLoadingApplicationRecordsApi,
  type ApiAppRef,
} from "opendcs-api";
import { LoadingAppsTable } from "./LoadingAppsTable";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useApi } from "../../contexts/app/ApiContext";

export const LoadingAppsPage: React.FC = () => {
  const [apps, setApps] = useState<ApiAppRef[]>([]);
  const [stale, setStale] = useState(true);
  const api = useApi();
  const appsApi = useMemo(
    () => new RESTLoadingApplicationRecordsApi(api.conf),
    [api.conf],
  );

  useEffect(() => {
    const fetchApps = async () => {
      const refs = await appsApi.getAppRefs(api.org);
      setApps(refs);
      setStale(false);
    };
    if (stale === true) {
      fetchApps();
    }
  }, [stale, api.org, appsApi]);

  const getApp = useCallback(
    (appId: number) => appsApi.getApp(api.org, appId),
    [api.org, appsApi],
  );

  const saveApp = useCallback(
    (app: ApiLoadingApp): Promise<void> => {
      const appId = app.appId && app.appId > 0 ? app.appId : undefined;
      // Return the POST promise so the table wrapper can await it before
      // transitioning the detail back to show mode (avoids save→refetch race).
      return appsApi
        .postApp(api.org, { ...app, appId })
        .then(() => setStale(true))
        .catch((e: unknown) => {
          console.error("Failed to save loading app", e);
        });
    },
    [api.org, appsApi],
  );

  const deleteApp = useCallback(
    (appId: number) => {
      appsApi
        .deleteApp(api.org, appId)
        .then(() => setStale(true))
        .catch((e: unknown) => console.error("Failed to delete loading app", e));
    },
    [api.org, appsApi],
  );

  return (
    <LoadingAppsTable
      apps={apps}
      loading={stale}
      getApp={getApp}
      actions={{ save: saveApp, remove: deleteApp }}
    />
  );
};