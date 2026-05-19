import { ComputationsTable } from "./ComputationsTable";
import { useAppRefsQuery } from "../../../queries/apps";
import { useTsGroupRefsQuery } from "../../../queries/tsGroups";
import { useFetchAlgorithm } from "../../../queries/algorithms";
import {
  useComputationsQuery,
  useDeleteComputationMutation,
  useFetchComputation,
  useSaveComputationMutation,
} from "../../../queries/computations";

export const Computations: React.FC = () => {
  const { data: computations = [], isLoading } = useComputationsQuery();
  const { data: processOptions = [] } = useAppRefsQuery();
  const { data: groupOptions = [] } = useTsGroupRefsQuery();
  const fetchComputation = useFetchComputation();
  const fetchAlgorithm = useFetchAlgorithm();
  const saveComputation = useSaveComputationMutation();
  const deleteComputation = useDeleteComputationMutation();

  return (
    <div className="content">
      <ComputationsTable
        computations={computations}
        loading={isLoading}
        getComputation={fetchComputation}
        getAlgorithm={fetchAlgorithm}
        actions={{
          // mutateAsync so AppDataTable awaits the save before transitioning
          // out of edit mode. postComputation returns the saved entity;
          // SaveAction expects Promise<void>.
          save: (computation) =>
            saveComputation.mutateAsync(computation).then(() => {}),
          remove: (computationId) => deleteComputation.mutate(computationId),
        }}
        processOptions={processOptions}
        groupOptions={groupOptions}
      />
    </div>
  );
};
