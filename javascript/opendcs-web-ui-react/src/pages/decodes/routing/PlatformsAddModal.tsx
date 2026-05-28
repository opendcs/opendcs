import { useCallback, useMemo, useState, type ReactNode } from "react";
import { Button, Modal, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiPlatformRef } from "opendcs-api";
import { ChooserTable, type ChooserColumnDef } from "../../../components/data-table";

export interface PlatformsAddModalProps {
  show: boolean;
  onHide: () => void;
  /** All available platforms (the modal filters out already-selected). */
  platforms: ApiPlatformRef[];
  loading?: boolean;
  /** Names of platforms already attached to the routing spec. */
  alreadySelectedNames: string[];
  onAdd: (names: string[]) => void;
}

export const PlatformsAddModal: React.FC<PlatformsAddModalProps> = ({
  show,
  onHide,
  platforms,
  loading = false,
  alreadySelectedNames,
  onAdd,
}) => {
  const [t] = useTranslation(["routing", "platforms", "translation"]);
  const [selectedNames, setSelectedNames] = useState<string[]>([]);

  // Reset selection each time the modal opens (matches SelectorModal pattern).
  const [wasShown, setWasShown] = useState(show);
  if (show !== wasShown) {
    setWasShown(show);
    if (show) setSelectedNames([]);
  }

  const available = useMemo(() => {
    const taken = new Set(alreadySelectedNames);
    return platforms.filter((p) => p.name !== undefined && !taken.has(p.name));
  }, [platforms, alreadySelectedNames]);

  const columns = useMemo<ChooserColumnDef<ApiPlatformRef>[]>(
    () => [
      { data: "platformId", header: t("platforms:header.Id"), type: "num" },
      { data: "name", header: t("platforms:header.Platform") },
      {
        data: "agency",
        header: t("platforms:header.Agency"),
        defaultContent: "",
      },
      {
        data: null,
        header: t("platforms:header.TransportId"),
        defaultContent: "",
        render: (_d, _t, row) =>
          row.transportMedia ? Object.values(row.transportMedia).join(", ") : "",
      },
      {
        data: "config",
        header: t("platforms:header.Config"),
        defaultContent: "",
      },
      {
        data: "description",
        header: t("platforms:header.Description"),
        defaultContent: "",
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
    body = <p>{t("routing:add_platforms_none")}</p>;
  } else {
    body = (
      <ChooserTable<ApiPlatformRef, string>
        data={available}
        getId={(p) => p.name ?? ""}
        columns={columns}
        mode="multi"
        selectedIds={selectedNames}
        onSelectionChange={setSelectedNames}
        selectAllAriaLabel={t("routing:select_all_platforms")}
        rowSelectAriaLabel={(p) =>
          t("routing:select_platform", { name: p.name ?? p.platformId })
        }
      />
    );
  }

  return (
    <Modal show={show} onHide={onHide} centered size="xl">
      <Modal.Header closeButton>
        <Modal.Title>{t("routing:add_platforms")}</Modal.Title>
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
          {t("routing:add_selected", { count: selectedNames.length })}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default PlatformsAddModal;
