import { ConfigsTable } from "./ConfigsTable";
import {
  useConfigsQuery,
  useDeleteConfigMutation,
  useFetchConfig,
  useSaveConfigMutation,
} from "../../../queries/configs";

export const ConfigsPage: React.FC = () => {
  const { data: configs = [], isFetching } = useConfigsQuery();
  const fetchConfig = useFetchConfig();
  const saveConfig = useSaveConfigMutation();
  const deleteConfig = useDeleteConfigMutation();

  return (
    <div className="content">
      <ConfigsTable
        configs={configs}
        loading={isFetching}
        getConfig={fetchConfig}
        actions={{
          save: async (config) => {
            await saveConfig.mutateAsync(config);
          },
          remove: (configId) => deleteConfig.mutate(configId),
        }}
      />
    </div>
  );
};

export default ConfigsPage;
