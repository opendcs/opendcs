import { useCallback, useState } from "react";
import { Alert, Button, Form, Modal, Spinner, Table } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import {
  useAlgorithmCatalogQuery,
  useImportAlgorithmsMutation,
} from "../../../queries/algorithms";

interface Props {
  show: boolean;
  onHide: () => void;
  onImported: () => void;
}

export const CheckForNewModal: React.FC<Props> = ({ show, onHide, onImported }) => {
  const [t] = useTranslation(["algorithms"]);
  const [selected, setSelected] = useState<Set<string>>(new Set());

  const handleHide = useCallback(() => {
    setSelected(new Set());
    onHide();
  }, [onHide]);

  const {
    data: catalog = [],
    isLoading,
    isError: catalogError,
  } = useAlgorithmCatalogQuery(show);

  const available = catalog.filter((a) => !a.alreadyImported);

  const importMutation = useImportAlgorithmsMutation({
    onSuccess: () => {
      onImported();
      handleHide();
    },
  });

  const toggleSelect = useCallback((execClass: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(execClass)) next.delete(execClass);
      else next.add(execClass);
      return next;
    });
  }, []);

  const toggleAll = useCallback(() => {
    setSelected((prev) =>
      prev.size === available.length
        ? new Set()
        : new Set(available.map((a) => a.execClass)),
    );
  }, [available]);

  const importSelected = useCallback(() => {
    if (selected.size === 0) return;
    importMutation.mutate([...selected]);
  }, [selected, importMutation]);

  return (
    <Modal show={show} onHide={handleHide} centered size="xl">
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
        {isLoading && (
          <div className="text-center p-4">
            <Spinner animation="border" />
          </div>
        )}
        {!isLoading && available.length === 0 && (
          <p>{t("algorithms:check_new.none_found")}</p>
        )}
        {!isLoading && available.length > 0 && (
          <Table striped bordered hover>
            <thead>
              <tr>
                <th style={{ width: "2.5rem" }}>
                  <Form.Check
                    type="checkbox"
                    checked={selected.size === available.length && available.length > 0}
                    onChange={toggleAll}
                    aria-label={t("algorithms:check_new.select_all")}
                  />
                </th>
                <th>{t("algorithms:header.Name")}</th>
                <th>{t("algorithms:header.ExecClass")}</th>
                <th>{t("algorithms:header.Description")}</th>
              </tr>
            </thead>
            <tbody>
              {available.map((algo) => (
                <tr
                  key={algo.execClass}
                  onClick={() => toggleSelect(algo.execClass)}
                  style={{ cursor: "pointer" }}
                >
                  <td>
                    <Form.Check
                      type="checkbox"
                      checked={selected.has(algo.execClass)}
                      readOnly
                      aria-label={algo.name}
                    />
                  </td>
                  <td>{algo.name}</td>
                  <td>{algo.execClass}</td>
                  <td>{algo.description}</td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={handleHide}>
          {t("algorithms:check_new.close")}
        </Button>
        <Button
          variant="primary"
          onClick={importSelected}
          disabled={selected.size === 0 || importMutation.isPending}
        >
          {importMutation.isPending ? (
            <Spinner animation="border" size="sm" />
          ) : (
            t("algorithms:check_new.import_selected", { count: selected.size })
          )}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};
