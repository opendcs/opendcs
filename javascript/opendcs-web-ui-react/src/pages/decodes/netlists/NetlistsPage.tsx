import { NetlistsTable } from "./NetlistsTable";
import {
  useDeleteNetlistMutation,
  useFetchNetlist,
  useNetlistRefsQuery,
  useSaveNetlistMutation,
} from "../../../queries/netlists";

export const NetlistsPage: React.FC = () => {
  const { data: netlists = [], isFetching } = useNetlistRefsQuery();
  const fetchNetlist = useFetchNetlist();
  const saveNetlist = useSaveNetlistMutation();
  const deleteNetlist = useDeleteNetlistMutation();

  return (
    <div className="content">
      <NetlistsTable
        netlists={netlists}
        loading={isFetching}
        getNetlist={fetchNetlist}
        actions={{
          save: async (netlist) => {
            await saveNetlist.mutateAsync(netlist);
          },
          remove: (netlistId) => deleteNetlist.mutate(netlistId),
        }}
      />
    </div>
  );
};

export default NetlistsPage;
