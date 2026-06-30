import { useCallback, useMemo, useState, type ReactNode } from "react";
import { Button, Modal, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { ChooserTable, type ChooserColumnDef } from "./ChooserTable";

export interface SelectorModalProps<T, TId extends string | number> {
  show: boolean;
  onHide: () => void;
  onSelect: (item: T) => void | Promise<void>;
  title: ReactNode;
  noneMessage: ReactNode;
  data: T[];
  loading?: boolean;
  getId: (item: T) => TId;
  columns: ChooserColumnDef<T>[];
}

/**
 * Single-select modal picker built on `ChooserTable`. The shared scaffold for
 * Algorithm / Site / Config select modals — keeps the open-reset, selection
 * state, body switching (loading / empty / chooser), and Cancel/Select footer
 * in one place so concrete pickers stay one-screen wrappers.
 */
export function SelectorModal<T, TId extends string | number>({
  show,
  onHide,
  onSelect,
  title,
  noneMessage,
  data,
  loading = false,
  getId,
  columns,
}: Readonly<SelectorModalProps<T, TId>>): React.ReactElement {
  const [t] = useTranslation(["translation"]);
  const [selectedIds, setSelectedIds] = useState<TId[]>([]);
  const [selecting, setSelecting] = useState(false);

  // Reset selection each time the modal opens. In-render transition compare
  // (per React docs) avoids the setState-in-effect anti-pattern.
  const [wasShown, setWasShown] = useState(show);
  if (show !== wasShown) {
    setWasShown(show);
    if (show) {
      setSelectedIds([]);
      setSelecting(false);
    }
  }

  const selectedId = selectedIds[0];
  const selected = useMemo<T | null>(() => {
    if (selectedId === undefined) return null;
    return data.find((d) => getId(d) === selectedId) ?? null;
  }, [data, selectedId, getId]);

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

  let body: ReactNode;
  if (loading && data.length === 0) {
    body = (
      <div className="text-center p-4">
        <Spinner animation="border" role="status" />
      </div>
    );
  } else if (data.length === 0) {
    body = <p>{noneMessage}</p>;
  } else {
    body = (
      <ChooserTable<T, TId>
        data={data}
        getId={getId}
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
        <Modal.Title>{title}</Modal.Title>
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
}
