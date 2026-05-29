import { DataSourcesTable } from "./DataSourcesTable";
import {
  useDataSourceRefsQuery,
  useDeleteDataSourceMutation,
  useFetchDataSource,
  useSaveDataSourceMutation,
} from "../../../queries/dataSources";

export const DataSourcesPage: React.FC = () => {
  const { data: dataSources = [], isFetching } = useDataSourceRefsQuery();
  const fetchDataSource = useFetchDataSource();
  const saveDataSource = useSaveDataSourceMutation();
  const deleteDataSource = useDeleteDataSourceMutation();

  return (
    <div className="content">
      <DataSourcesTable
        dataSources={dataSources}
        loading={isFetching}
        getDataSource={fetchDataSource}
        actions={{
          save: async (dataSource) => {
            await saveDataSource.mutateAsync(dataSource);
          },
          remove: (dataSourceId) => deleteDataSource.mutate(dataSourceId),
        }}
      />
    </div>
  );
};

export default DataSourcesPage;
