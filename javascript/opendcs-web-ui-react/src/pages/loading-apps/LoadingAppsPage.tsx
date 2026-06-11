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
  const { data: appStats = [], isError: isStatError } = useAppStatQuery();
  const fetchApp = useFetchApp();
  const saveApp = useSaveAppMutation();
  const deleteApp = useDeleteAppMutation();

  const appsWithStatus = useMemo(
    () =>
      apps.map((app) => {
        const stat = appStats.find((s) => s.appId === app.appId);
        return {
          ...app,
          _pid: stat?.pid ?? null,
          _status: stat?.status ?? null,
        };
      }),
    [apps, appStats],
  );

  return (
    <div className="content">
      {isStatError && (
        <div className="alert alert-warning" role="alert">
          Process status is unavailable — app statuses may not reflect current state.
        </div>
      )}
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
