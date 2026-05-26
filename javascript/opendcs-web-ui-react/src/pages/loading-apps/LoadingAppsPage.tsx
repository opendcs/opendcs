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

  return (
    <div className="content">
      <LoadingAppsTable
        apps={apps}
        appStats={appStats}
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
