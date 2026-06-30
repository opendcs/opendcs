import { useCallback, useMemo, useState, type ReactNode } from "react";
import { Button, Modal, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { ChooserTable, type ChooserColumnDef } from "./ChooserTable";

export interface MultiSelectorModalProps<T, TId extends string | number> {
  show: boolean;
  onHide: () => void;
  onAdd: (ids: TId[]) => void;
  title: ReactNode;
  noneMessage: ReactNode;
  /** Footer confirm-button label; receives the current selection count. */
  confirmLabel: (count: number) => ReactNode;
  data: T[];
  loading?: boolean;
  getId: (item: T) => TId;
  columns: ChooserColumnDef<T>[];
  /** Ids already attached — filtered out of the chooser. */
  excludeIds?: TId[];
  selectAllAriaLabel?: string;
  rowSelectAriaLabel?: (item: T) => string;
  size?: "lg" | "xl";
  /** Footer cancel-button label. Defaults to the shared `translation:cancel`. */
  cancelLabel?: ReactNode;
  /** Content rendered above the chooser (e.g. error alerts). */
  banner?: ReactNode;
  /** Show a spinner on the confirm button and disable it (async in flight). */
  confirmPending?: boolean;
  /**
   * Whether confirming closes the modal. Defaults to `true`. Set `false` when
   * the caller drives an async action and closes itself on success.
   */
  closeOnConfirm?: boolean;
}

/**
 * Multi-select modal picker built on `ChooserTable` — the multi-select sibling
 * of `SelectorModal`. Owns the open-reset, selection state, available-filtering,
 * body switching (loading / empty / chooser) and Cancel/Add footer so concrete
 * pickers stay one-screen wrappers that only supply columns and labels.
 */
export function MultiSelectorModal<T, TId extends string | number>({
  show,
  onHide,
  onAdd,
  title,
  noneMessage,
  confirmLabel,
  data,
  loading = false,
  getId,
  columns,
  excludeIds = [],
  selectAllAriaLabel,
  rowSelectAriaLabel,
  size = "xl",
  cancelLabel,
  banner,
  confirmPending = false,
  closeOnConfirm = true,
}: Readonly<MultiSelectorModalProps<T, TId>>): React.ReactElement {
  const [t] = useTranslation(["translation"]);
  const [selectedIds, setSelectedIds] = useState<TId[]>([]);

  // Reset selection each time the modal opens (matches SelectorModal pattern).
  const [wasShown, setWasShown] = useState(show);
  if (show !== wasShown) {
    setWasShown(show);
    if (show) setSelectedIds([]);
  }

  const available = useMemo(() => {
    const taken = new Set(excludeIds.map(String));
    return data.filter((d) => !taken.has(String(getId(d))));
  }, [data, excludeIds, getId]);

  const handleAdd = useCallback(() => {
    if (selectedIds.length === 0) return;
    onAdd(selectedIds);
    if (closeOnConfirm) onHide();
  }, [selectedIds, onAdd, onHide, closeOnConfirm]);

  let body: ReactNode;
  if (loading && available.length === 0) {
    body = (
      <div className="text-center p-4">
        <Spinner animation="border" role="status" />
      </div>
    );
  } else if (available.length === 0) {
    body = <p>{noneMessage}</p>;
  } else {
    body = (
      <ChooserTable<T, TId>
        data={available}
        getId={getId}
        columns={columns}
        mode="multi"
        selectedIds={selectedIds}
        onSelectionChange={setSelectedIds}
        selectAllAriaLabel={selectAllAriaLabel}
        rowSelectAriaLabel={rowSelectAriaLabel}
      />
    );
  }

  return (
    <Modal show={show} onHide={onHide} centered size={size}>
      <Modal.Header closeButton>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body style={{ maxHeight: "60vh", overflowY: "auto" }}>
        {banner}
        {body}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          {cancelLabel ?? t("translation:cancel")}
        </Button>
        <Button
          variant="primary"
          onClick={handleAdd}
          disabled={selectedIds.length === 0 || confirmPending}
        >
          {confirmPending ? (
            <Spinner animation="border" size="sm" />
          ) : (
            confirmLabel(selectedIds.length)
          )}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

export default MultiSelectorModal;
