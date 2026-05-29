import { PresentationsTable } from "./PresentationsTable";
import {
  useDeletePresentationMutation,
  useFetchPresentation,
  usePresentationRefsQuery,
  useSavePresentationMutation,
} from "../../../queries/presentations";

export const PresentationsPage: React.FC = () => {
  const { data: presentations = [], isFetching } = usePresentationRefsQuery();
  const fetchPresentation = useFetchPresentation();
  const savePresentation = useSavePresentationMutation();
  const deletePresentation = useDeletePresentationMutation();

  return (
    <div className="content">
      <PresentationsTable
        presentations={presentations}
        loading={isFetching}
        getPresentation={fetchPresentation}
        actions={{
          save: async (presentation) => {
            await savePresentation.mutateAsync(presentation);
          },
          remove: (groupId) => deletePresentation.mutate(groupId),
        }}
      />
    </div>
  );
};

export default PresentationsPage;
