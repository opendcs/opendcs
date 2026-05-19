import { useCallback, useMemo, useState, type ReactNode } from "react";
import { Button, Modal, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiAlgorithmRef } from "opendcs-api";
import { useAlgorithmsQuery } from "../../../queries/algorithms";
import { ChooserTable, type ChooserColumnDef } from "../../../components/data-table";

interface Props {
  show: boolean;
  onHide: () => void;
  onSelect: (algo: ApiAlgorithmRef) => void | Promise<void>;
}

export const AlgorithmSelectModal: React.FC<Props> = ({ show, onHide, onSelect }) => {
  const [t] = useTranslation(["computations", "translation"]);
  const { data: algorithms = [], isFetching } = useAlgorithmsQuery();

  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [selecting, setSelecting] = useState(false);

  // Reset selection each time the modal opens. In-render reset on a prop
  // transition (per React docs) avoids setState-in-effect.
  const [wasShown, setWasShown] = useState(show);
  if (show !== wasShown) {
    setWasShown(show);
    if (show) {
      setSelectedIds([]);
      setSelecting(false);
    }
  }

  const selectedId = selectedIds[0];
  const selected = useMemo<ApiAlgorithmRef | null>(() => {
    if (selectedId === undefined) return null;
    return algorithms.find((a) => a.algorithmId === selectedId) ?? null;
  }, [algorithms, selectedId]);

  const handleSelect = useCallback(async () => {
    if (!selected) return;
    setSelecting(true);
    try {
      await onSelect(selected);
      onHide();
    } finally {
      setSelecting(false);
    }
  }, [selected, onSelect, onHide]);

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

  let body: ReactNode;
  if (isFetching && algorithms.length === 0) {
    body = (
      <div className="text-center p-4">
        <Spinner animation="border" />
      </div>
    );
  } else if (algorithms.length === 0) {
    body = <p>{t("computations:editor.select_algorithm_none")}</p>;
  } else {
    body = (
      <ChooserTable<ApiAlgorithmRef, number>
        data={algorithms}
        getId={(a) => a.algorithmId ?? -1}
        columns={columns}
        mode="single"
        selectedIds={selectedIds}
        onSelectionChange={setSelectedIds}
      />
    );
  }

  return (
    <Modal show={show} onHide={onHide} centered size="xl">
      <Modal.Header closeButton>
        <Modal.Title>{t("computations:editor.select_algorithm_title")}</Modal.Title>
      </Modal.Header>
      <Modal.Body style={{ maxHeight: "60vh", overflowY: "auto" }}>{body}</Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          {t("translation:cancel")}
        </Button>
        <Button
          variant="primary"
          onClick={handleSelect}
          disabled={selected === null || selecting}
        >
          {selecting ? (
            <Spinner animation="border" size="sm" />
          ) : (
            t("translation:select")
          )}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};
