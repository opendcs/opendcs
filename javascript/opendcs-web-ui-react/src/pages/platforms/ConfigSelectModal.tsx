import { useCallback, useMemo, useState, type ReactNode } from "react";
import { Button, Form, Modal, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiConfigRef } from "opendcs-api";
import { usePlatformConfigsQuery } from "../../queries/platforms";
import { ChooserTable, type ChooserColumnDef } from "../../components/data-table";

interface Props {
  show: boolean;
  onHide: () => void;
  onSelect: (config: ApiConfigRef) => void;
}

export const ConfigSelectModal: React.FC<Props> = ({ show, onHide, onSelect }) => {
  const [t] = useTranslation(["platforms", "translation"]);
  const { data: configs = [], isFetching } = usePlatformConfigsQuery();
  const [filter, setFilter] = useState("");
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // Reset modal-local state each time the modal opens. Per React docs, prefer
  // in-render reset on a prop transition over a useEffect+setState pair.
  const [wasShown, setWasShown] = useState(show);
  if (show && !wasShown) {
    setWasShown(true);
    setFilter("");
    setSelectedIds([]);
  } else if (!show && wasShown) {
    setWasShown(false);
  }

  const filtered = useMemo(() => {
    const q = filter.trim().toLowerCase();
    if (!q) return configs;
    return configs.filter(
      (c) =>
        (c.name ?? "").toLowerCase().includes(q) ||
        (c.description ?? "").toLowerCase().includes(q),
    );
  }, [configs, filter]);

  const selectedId = selectedIds[0];
  const selected = useMemo<ApiConfigRef | null>(() => {
    if (selectedId === undefined) return null;
    return configs.find((c) => c.configId === selectedId) ?? null;
  }, [configs, selectedId]);

  const handleSelect = useCallback(() => {
    if (!selected) return;
    onSelect(selected);
    onHide();
  }, [selected, onSelect, onHide]);

  const columns: ChooserColumnDef<ApiConfigRef>[] = useMemo(
    () => [
      { data: "configId", header: t("platforms:header.Id"), type: "num" },
      { data: "name", header: t("platforms:config") },
      {
        data: "numPlatforms",
        header: t("platforms:select_config_num_platforms"),
        type: "num",
        defaultContent: "0",
      },
      {
        data: "description",
        header: t("platforms:header.Description"),
        defaultContent: "",
      },
    ],
    [t],
  );

  let body: ReactNode;
  if (isFetching && configs.length === 0) {
    body = (
      <div className="text-center p-4">
        <Spinner animation="border" />
      </div>
    );
  } else if (filtered.length === 0) {
    body = <p>{t("platforms:select_config_none")}</p>;
  } else {
    body = (
      <div style={{ maxHeight: "45vh", overflowY: "auto" }}>
        <ChooserTable<ApiConfigRef, number>
          data={filtered}
          getId={(c) => c.configId ?? -1}
          columns={columns}
          mode="single"
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onRowDoubleClick={(c) => {
            onSelect(c);
            onHide();
          }}
          dataTableOptions={{
            paging: false,
            info: false,
            layout: {
              topStart: null,
              topEnd: null,
              bottomStart: null,
              bottomEnd: null,
            },
          }}
        />
      </div>
    );
  }

  return (
    <Modal show={show} onHide={onHide} centered size="xl">
      <Modal.Header closeButton>
        <Modal.Title>{t("platforms:select_config")}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Form.Control
          type="search"
          placeholder={t("platforms:select_config_filter")}
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="mb-3"
          aria-label={t("platforms:select_config_filter")}
        />
        {body}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          {t("translation:cancel")}
        </Button>
        <Button variant="primary" onClick={handleSelect} disabled={!selected}>
          {t("translation:select")}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};
