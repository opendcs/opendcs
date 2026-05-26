import { useCallback, useMemo, useState, type ReactNode } from "react";
import { Alert, Button, Modal, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { ChooserTable, type ChooserColumnDef } from "../../../components/data-table";
import {
  useAlgorithmCatalogQuery,
  useImportAlgorithmsMutation,
  type CatalogAlgorithm,
} from "../../../queries/algorithms";

interface Props {
  show: boolean;
  onHide: () => void;
  onImported: () => void;
}

export const CheckForNewModal: React.FC<Props> = ({ show, onHide, onImported }) => {
  const [t] = useTranslation(["algorithms", "translation"]);
  const [selectedExecClasses, setSelectedExecClasses] = useState<string[]>([]);

  // Reset selection each time the modal opens — in-render transition compare
  // avoids the setState-in-effect anti-pattern (same approach as SelectorModal).
  const [wasShown, setWasShown] = useState(show);
  if (show !== wasShown) {
    setWasShown(show);
    if (show) setSelectedExecClasses([]);
  }

  const {
    data: catalog = [],
    isLoading,
    isError: catalogError,
  } = useAlgorithmCatalogQuery(show);

  const available = catalog.filter((a) => !a.alreadyImported);

  const importMutation = useImportAlgorithmsMutation({
    onSuccess: () => {
      onImported();
      onHide();
    },
  });

  const importSelected = useCallback(() => {
    if (selectedExecClasses.length === 0) return;
    importMutation.mutate(selectedExecClasses);
  }, [selectedExecClasses, importMutation]);

  const columns: ChooserColumnDef<CatalogAlgorithm>[] = useMemo(
    () => [
      { data: "name", header: t("algorithms:header.Name") },
      { data: "execClass", header: t("algorithms:header.ExecClass") },
      { data: "description", header: t("algorithms:header.Description") },
    ],
    [t],
  );

  let body: ReactNode;
  if (isLoading) {
    body = (
      <div className="text-center p-4">
        <Spinner animation="border" />
      </div>
    );
  } else if (available.length === 0) {
    body = <p>{t("algorithms:check_new.none_found")}</p>;
  } else {
    body = (
      <ChooserTable<CatalogAlgorithm, string>
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
      <Modal.Body style={{ maxHeight: "60vh", overflowY: "auto" }}>
        {catalogError && (
          <Alert variant="danger">{t("algorithms:check_new.fetch_error")}</Alert>
        )}
        {importMutation.isError && (
          <Alert variant="danger">{t("algorithms:check_new.import_error")}</Alert>
        )}
        {body}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          {t("algorithms:check_new.close")}
        </Button>
        <Button
          variant="primary"
          onClick={importSelected}
          disabled={selectedExecClasses.length === 0 || importMutation.isPending}
        >
          {importMutation.isPending ? (
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
