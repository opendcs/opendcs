import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiAlgorithmRef } from "opendcs-api";
import { useAlgorithmsQuery } from "../../../queries/algorithms";
import { SelectorModal, type ChooserColumnDef } from "../../../components/data-table";

interface Props {
  show: boolean;
  onHide: () => void;
  onSelect: (algo: ApiAlgorithmRef) => void | Promise<void>;
}

export const AlgorithmSelectModal: React.FC<Props> = ({ show, onHide, onSelect }) => {
  const [t] = useTranslation(["computations", "translation"]);
  const { data: algorithms = [], isFetching } = useAlgorithmsQuery();

  const columns: ChooserColumnDef<ApiAlgorithmRef>[] = useMemo(
    () => [
      { data: "algorithmId", header: t("computations:header.Id"), type: "num" },
      { data: "algorithmName", header: t("computations:editor.name") },
      {
        data: "execClass",
        header: t("computations:editor.select_algorithm_exec_class"),
      },
      {
        data: "description",
        header: t("computations:header.Description"),
        defaultContent: "",
      },
    ],
    [t],
  );

  return (
    <SelectorModal<ApiAlgorithmRef, number>
      show={show}
      onHide={onHide}
      onSelect={onSelect}
      title={t("computations:editor.select_algorithm_title")}
      noneMessage={t("computations:editor.select_algorithm_none")}
      data={algorithms}
      loading={isFetching}
      getId={(a) => a.algorithmId ?? -1}
      columns={columns}
    />
  );
};
