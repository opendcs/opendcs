import { SchedulesTable } from "./SchedulesTable";
import {
  useDeleteScheduleMutation,
  useFetchSchedule,
  useSaveScheduleMutation,
  useScheduleRefsQuery,
} from "../../queries/scheduleEntries";
import { useAppRefsQuery } from "../../queries/apps";
import { useRoutingsQuery } from "../../queries/routing";

export const SchedulesPage: React.FC = () => {
  const { data: schedules = [], isFetching } = useScheduleRefsQuery();
  const { data: apps = [] } = useAppRefsQuery();
  const { data: routings = [], isFetching: routingsFetching } = useRoutingsQuery();
  const fetchSchedule = useFetchSchedule();
  const saveSchedule = useSaveScheduleMutation();
  const deleteSchedule = useDeleteScheduleMutation();

  return (
    <div className="content">
      <SchedulesTable
        schedules={schedules}
        apps={apps}
        routings={routings}
        routingsLoading={routingsFetching}
        loading={isFetching}
        getSchedule={fetchSchedule}
        actions={{
          save: async (schedule) => {
            await saveSchedule.mutateAsync(schedule);
          },
          remove: (schedEntryId) => deleteSchedule.mutate(schedEntryId),
        }}
      />
    </div>
  );
};

export default SchedulesPage;
