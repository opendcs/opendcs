import { SchedulesTable } from "./SchedulesTable";
import {
  useDeleteScheduleMutation,
  useFetchSchedule,
  useSaveScheduleMutation,
  useScheduleRefsQuery,
} from "../../queries/scheduleEntries";

export const SchedulesPage: React.FC = () => {
  const { data: schedules = [], isFetching } = useScheduleRefsQuery();
  const fetchSchedule = useFetchSchedule();
  const saveSchedule = useSaveScheduleMutation();
  const deleteSchedule = useDeleteScheduleMutation();

  return (
    <div className="content">
      <SchedulesTable
        schedules={schedules}
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
