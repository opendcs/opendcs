import { TsGroupsTable } from "./TsGroupsTable";
import {
  useDeleteTsGroupMutation,
  useFetchTsGroup,
  useSaveTsGroupMutation,
  useTsGroupRefsQuery,
} from "../../../queries/tsGroups";
import { useTimeSeriesRefsQuery } from "../../../queries/timeSeries";
import { useDataTypeListQuery } from "../../../queries/dataTypes";

export const TsGroupsPage: React.FC = () => {
  const { data: groups = [], isFetching } = useTsGroupRefsQuery();
  // Prefetched here rather than inside the chooser so opening "Add" on a group
  // doesn't stall on a catalog request.
  const { data: timeSeries = [], isFetching: timeSeriesLoading } =
    useTimeSeriesRefsQuery();
  // Prefetched for the same reason: the criteria table labels data types the
  // API returns by id alone, and each detail row renders in its own React root.
  const { data: dataTypes = [] } = useDataTypeListQuery();
  const fetchGroup = useFetchTsGroup();
  const saveGroup = useSaveTsGroupMutation();
  const deleteGroup = useDeleteTsGroupMutation();

  return (
    <div className="content">
      <TsGroupsTable
        groups={groups}
        loading={isFetching}
        getGroup={fetchGroup}
        timeSeries={timeSeries}
        timeSeriesLoading={timeSeriesLoading}
        dataTypes={dataTypes}
        actions={{
          save: async (group) => {
            await saveGroup.mutateAsync(group);
          },
          remove: (groupId) => deleteGroup.mutate(groupId),
        }}
      />
    </div>
  );
};

export default TsGroupsPage;
