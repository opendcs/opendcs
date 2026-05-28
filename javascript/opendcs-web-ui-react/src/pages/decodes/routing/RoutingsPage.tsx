import { RoutingsTable } from "./RoutingsTable";
import {
  useDeleteRoutingMutation,
  useFetchRouting,
  useRoutingsQuery,
  useSaveRoutingMutation,
} from "../../../queries/routing";
import { usePlatformsQuery } from "../../../queries/platforms";
import { useNetlistRefsQuery } from "../../../queries/netlists";

export const RoutingsPage: React.FC = () => {
  const { data: routings = [], isFetching } = useRoutingsQuery();
  const { data: platforms = [], isFetching: platformsFetching } = usePlatformsQuery();
  const { data: netlists = [], isFetching: netlistsFetching } = useNetlistRefsQuery();
  const fetchRouting = useFetchRouting();
  const saveRouting = useSaveRoutingMutation();
  const deleteRouting = useDeleteRoutingMutation();

  return (
    <div className="content">
      <RoutingsTable
        routings={routings}
        platforms={platforms}
        platformsLoading={platformsFetching}
        netlists={netlists}
        netlistsLoading={netlistsFetching}
        loading={isFetching}
        getRouting={fetchRouting}
        actions={{
          save: async (routing) => {
            await saveRouting.mutateAsync(routing);
          },
          remove: (routingId) => deleteRouting.mutate(routingId),
        }}
      />
    </div>
  );
};

export default RoutingsPage;
