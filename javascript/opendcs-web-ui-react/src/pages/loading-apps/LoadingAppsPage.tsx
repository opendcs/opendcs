import { useMemo } from "react";
import { LoadingAppsTable } from "./LoadingAppsTable";
import {
  useAppRefsQuery,
  useAppStatQuery,
  useDeleteAppMutation,
  useFetchApp,
  useSaveAppMutation,
} from "../../queries/apps";

export const LoadingAppsPage: React.FC = () => {
  const { data: apps = [], isLoading } = useAppRefsQuery();
  const { data: appStats = [] } = useAppStatQuery();
  const fetchApp = useFetchApp();
  const saveApp = useSaveAppMutation();
  const deleteApp = useDeleteAppMutation();

  const appsWithStatus = useMemo(
    () =>
      apps.map((app) => ({
        ...app,
        _pid: appStats.find((s) => s.appId === app.appId)?.pid ?? null,
      })),
    [apps, appStats],
  );

  return (
    <div className="content">
      <LoadingAppsTable
        apps={appsWithStatus}
        loading={isLoading}
        getApp={fetchApp}
        actions={{
          // mutateAsync so AppDataTable awaits the save before transitioning
          // out of edit mode — avoids the save→refetch race the table relies on.
          save: (app) => saveApp.mutateAsync(app).then(() => {}),
          remove: (appId) => deleteApp.mutate(appId),
        }}
      />
    </div>
  );
};
