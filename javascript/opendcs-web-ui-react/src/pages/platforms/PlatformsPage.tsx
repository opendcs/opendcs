import { PlatformsTable } from "./PlatformsTable";
import {
  useDeletePlatformMutation,
  useFetchPlatform,
  usePlatformsQuery,
  useSavePlatformMutation,
} from "../../queries/platforms";
import { useFetchConfig } from "../../queries/configs";
import { useFetchSite } from "../../queries/sites";

export const PlatformsPage: React.FC = () => {
  const { data: platforms = [], isFetching } = usePlatformsQuery();
  const fetchPlatform = useFetchPlatform();
  const fetchSite = useFetchSite();
  const fetchConfig = useFetchConfig();
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
