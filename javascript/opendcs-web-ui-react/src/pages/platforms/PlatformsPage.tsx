import { PlatformsTable } from "./PlatformsTable";
import {
  useDeletePlatformMutation,
  useFetchPlatform,
  useFetchPlatformConfig,
  usePlatformsQuery,
  useSavePlatformMutation,
} from "../../queries/platforms";
import { useFetchSite } from "../../queries/sites";

export const PlatformsPage: React.FC = () => {
  const { data: platforms = [], isFetching } = usePlatformsQuery();
  const fetchPlatform = useFetchPlatform();
  const fetchSite = useFetchSite();
  const fetchConfig = useFetchPlatformConfig();
  const savePlatform = useSavePlatformMutation();
  const deletePlatform = useDeletePlatformMutation();

  return (
    <div className="content">
      <PlatformsTable
        platforms={platforms}
        loading={isFetching}
        getPlatform={fetchPlatform}
        getSite={fetchSite}
        getConfig={fetchConfig}
        actions={{
          save: (platform) => savePlatform.mutateAsync(platform),
          remove: (platformId) => deletePlatform.mutate(platformId),
        }}
      />
    </div>
  );
};
