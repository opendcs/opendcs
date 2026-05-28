import { useCallback, useMemo, useState, type ReactNode } from "react";
import { Button, Modal, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiNetlistRef } from "opendcs-api";
import { ChooserTable, type ChooserColumnDef } from "../../../components/data-table";

export interface NetlistsAddModalProps {
  show: boolean;
  onHide: () => void;
  /** All available network lists (the modal filters out already-selected). */
  netlists: ApiNetlistRef[];
  loading?: boolean;
  /** Names of network lists already attached to the routing spec. */
  alreadySelectedNames: string[];
  onAdd: (names: string[]) => void;
}

export const NetlistsAddModal: React.FC<NetlistsAddModalProps> = ({
  show,
  onHide,
  netlists,
  loading = false,
  alreadySelectedNames,
  onAdd,
}) => {
  const [t] = useTranslation(["routing", "translation"]);
  const [selectedNames, setSelectedNames] = useState<string[]>([]);

  // Reset selection each time the modal opens (matches SelectorModal pattern).
  const [wasShown, setWasShown] = useState(show);
  if (show !== wasShown) {
    setWasShown(show);
    if (show) setSelectedNames([]);
  }

  const available = useMemo(() => {
    const taken = new Set(alreadySelectedNames);
    return netlists.filter((n) => n.name !== undefined && !taken.has(n.name));
  }, [netlists, alreadySelectedNames]);

  const columns = useMemo<ChooserColumnDef<ApiNetlistRef>[]>(
    () => [
      { data: "name", header: t("routing:netlist_name") },
      {
        data: "transportMediumType",
        header: t("routing:netlist_tm_type"),
        defaultContent: "",
      },
      {
        data: "numPlatforms",
        header: t("routing:netlist_num_platforms"),
        type: "num",
        defaultContent: "0",
      },
    ],
    [t],
  );

  const handleAdd = useCallback(() => {
    if (selectedNames.length === 0) return;
    onAdd(selectedNames);
    onHide();
  }, [selectedNames, onAdd, onHide]);

  let body: ReactNode;
  if (loading && available.length === 0) {
    body = (
      <div className="text-center p-4">
        <Spinner animation="border" role="status" />
      </div>
    );
  } else if (available.length === 0) {
    body = <p>{t("routing:add_netlists_none")}</p>;
  } else {
    body = (
      <ChooserTable<ApiNetlistRef, string>
        data={available}
        getId={(n) => n.name ?? ""}
        columns={columns}
        mode="multi"
        selectedIds={selectedNames}
        onSelectionChange={setSelectedNames}
        selectAllAriaLabel={t("routing:select_all_netlists")}
        rowSelectAriaLabel={(n) => t("routing:select_netlist", { name: n.name })}
      />
    );
  }

  return (
    <Modal show={show} onHide={onHide} centered size="lg">
      <Modal.Header closeButton>
        <Modal.Title>{t("routing:add_netlists")}</Modal.Title>
      </Modal.Header>
      <Modal.Body style={{ maxHeight: "60vh", overflowY: "auto" }}>{body}</Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          {t("translation:cancel")}
        </Button>
        <Button
          variant="primary"
          onClick={handleAdd}
          disabled={selectedNames.length === 0}
        >
          {t("routing:add_selected_netlists", { count: selectedNames.length })}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default NetlistsAddModal;
