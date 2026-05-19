import { useCallback, useMemo, useState, type ReactNode } from "react";
import { Button, Form, Modal, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiSiteRef } from "opendcs-api";
import { useSitesQuery } from "../../queries/sites";
import { ChooserTable, type ChooserColumnDef } from "../../components/data-table";
import { siteDisplayName } from "./siteDisplayName";

interface Props {
  show: boolean;
  onHide: () => void;
  onSelect: (site: ApiSiteRef) => void;
}

export const SiteSelectModal: React.FC<Props> = ({ show, onHide, onSelect }) => {
  const [t] = useTranslation(["platforms", "translation"]);
  const { data: sites = [], isFetching } = useSitesQuery();
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
    if (!q) return sites;
    return sites.filter((s) => {
      const name = siteDisplayName(s).toLowerCase();
      const desc = (s.description ?? "").toLowerCase();
      return name.includes(q) || desc.includes(q);
    });
  }, [sites, filter]);

  const selectedId = selectedIds[0];
  const selected = useMemo<ApiSiteRef | null>(() => {
    if (selectedId === undefined) return null;
    return sites.find((s) => s.siteId === selectedId) ?? null;
  }, [sites, selectedId]);

  const handleSelect = useCallback(() => {
    if (!selected) return;
    onSelect(selected);
    onHide();
  }, [selected, onSelect, onHide]);

  const columns: ChooserColumnDef<ApiSiteRef>[] = useMemo(
    () => [
      { data: "siteId", header: t("platforms:header.Id"), type: "num" },
      {
        data: null,
        header: t("platforms:site"),
        render: (_d, _t, row) => siteDisplayName(row),
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
  if (isFetching && sites.length === 0) {
    body = (
      <div className="text-center p-4">
        <Spinner animation="border" />
      </div>
    );
  } else if (filtered.length === 0) {
    body = <p>{t("platforms:select_site_none")}</p>;
  } else {
    body = (
      <div style={{ maxHeight: "45vh", overflowY: "auto" }}>
        <ChooserTable<ApiSiteRef, number>
          data={filtered}
          getId={(s) => s.siteId ?? -1}
          columns={columns}
          mode="single"
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onRowDoubleClick={(s) => {
            onSelect(s);
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
        <Modal.Title>{t("platforms:select_site")}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Form.Control
          type="search"
          placeholder={t("platforms:select_site_filter")}
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="mb-3"
          aria-label={t("platforms:select_site_filter")}
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
