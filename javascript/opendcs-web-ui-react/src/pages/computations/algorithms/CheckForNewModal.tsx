import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { Button, Modal, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { useApi } from "../../../contexts/app/ApiContext";
import { ChooserTable, type ChooserColumnDef } from "../../../components/data-table";

interface AvailableAlgorithm {
  name: string;
  execClass: string;
  description: string;
  alreadyImported: boolean;
}

interface Props {
  show: boolean;
  onHide: () => void;
  onImported: () => void;
}

export const CheckForNewModal: React.FC<Props> = ({ show, onHide, onImported }) => {
  const [t] = useTranslation(["algorithms", "translation"]);
  const api = useApi();
  const [available, setAvailable] = useState<AvailableAlgorithm[]>([]);
  const [selectedExecClasses, setSelectedExecClasses] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);

  useEffect(() => {
    if (!show) return;
    let cancelled = false;
    setLoading(true);
    setSelectedExecClasses([]);
    fetch("/odcsapi/algorithmcatalog", {
      headers: { "X-ORGANIZATION-ID": api.org },
    })
      .then((res) => res.json())
      .then((data: AvailableAlgorithm[]) => {
        if (!cancelled) {
          setAvailable(data.filter((a) => !a.alreadyImported));
        }
      })
      .catch((e: unknown) => console.error("Failed to fetch algorithm catalog", e))
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [show, api.org]);

  const importSelected = useCallback(() => {
    if (selectedExecClasses.length === 0) return;
    setImporting(true);
    fetch("/odcsapi/algorithmcatalog", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-ORGANIZATION-ID": api.org,
      },
      body: JSON.stringify(selectedExecClasses),
    })
      .then((res) => {
        if (!res.ok) throw new Error(`Import failed: ${res.status}`);
        onImported();
        onHide();
      })
      .catch((e: unknown) => console.error("Failed to import algorithms", e))
      .finally(() => setImporting(false));
  }, [selectedExecClasses, api.org, onImported, onHide]);

  const columns: ChooserColumnDef<AvailableAlgorithm>[] = useMemo(
    () => [
      { data: "name", header: t("algorithms:header.Name") },
      { data: "execClass", header: t("algorithms:header.ExecClass") },
      { data: "description", header: t("algorithms:header.Description") },
    ],
    [t],
  );

  let body: ReactNode;
  if (loading) {
    body = (
      <div className="text-center p-4">
        <Spinner animation="border" />
      </div>
    );
  } else if (available.length === 0) {
    body = <p>{t("algorithms:check_new.none_found")}</p>;
  } else {
    body = (
      <ChooserTable<AvailableAlgorithm, string>
        data={available}
        getId={(a) => a.execClass}
        columns={columns}
        mode="multi"
        selectedIds={selectedExecClasses}
        onSelectionChange={setSelectedExecClasses}
        selectAllAriaLabel={t("algorithms:check_new.select_all")}
        rowSelectAriaLabel={(a) => a.name}
      />
    );
  }

  return (
    <Modal show={show} onHide={onHide} centered size="xl">
      <Modal.Header closeButton>
        <Modal.Title>{t("algorithms:check_new.title")}</Modal.Title>
      </Modal.Header>
      <Modal.Body style={{ maxHeight: "60vh", overflowY: "auto" }}>{body}</Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          {t("algorithms:check_new.close")}
        </Button>
        <Button
          variant="primary"
          onClick={importSelected}
          disabled={selectedExecClasses.length === 0 || importing}
        >
          {importing ? (
            <Spinner animation="border" size="sm" />
          ) : (
            t("algorithms:check_new.import_selected", {
              count: selectedExecClasses.length,
            })
          )}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};
