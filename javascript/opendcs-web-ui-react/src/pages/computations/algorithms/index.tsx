import { AlgorithmsTable } from "./AlgorithmsTable";
import {
  useAlgorithmsQuery,
  useDeleteAlgorithmMutation,
  useFetchAlgorithm,
  useFetchPropSpecs,
  useSaveAlgorithmMutation,
} from "../../../queries/algorithms";

export const Algorithms: React.FC = () => {
  const { data: algorithms = [], isFetching } = useAlgorithmsQuery();
  const fetchAlgorithm = useFetchAlgorithm();
  const fetchPropSpecs = useFetchPropSpecs();
  const saveAlgorithm = useSaveAlgorithmMutation();
  const deleteAlgorithm = useDeleteAlgorithmMutation();

  return (
    <div className="content">
      <AlgorithmsTable
        algorithms={algorithms}
        loading={isFetching}
        getAlgorithm={fetchAlgorithm}
        getPropSpecs={fetchPropSpecs}
        actions={{
          // mutateAsync so AppDataTable awaits the save before transitioning
          // out of edit mode — avoids the save→refetch race the table relies on.
          // postAlgorithm returns the saved entity; SaveAction expects Promise<void>.
          save: (algorithm) => saveAlgorithm.mutateAsync(algorithm).then(() => {}),
          remove: (algorithmId) => deleteAlgorithm.mutate(algorithmId),
        }}
      />
    </div>
  );
};
