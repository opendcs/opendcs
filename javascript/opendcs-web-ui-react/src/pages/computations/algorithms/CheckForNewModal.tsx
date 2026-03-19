import { useCallback, useEffect, useState } from "react";
import { Button, Form, Modal, Spinner, Table } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { useApi } from "../../../contexts/app/ApiContext";

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
  const [t] = useTranslation(["algorithms"]);
  const api = useApi();
  const [available, setAvailable] = useState<AvailableAlgorithm[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);

  useEffect(() => {
    if (!show) return;
    setLoading(true);
    setSelected(new Set());
    fetch("/odcsapi/algorithmcatalog", {
      headers: { "X-ORGANIZATION-ID": api.org },
    })
      .then((res) => res.json())
      .then((data: AvailableAlgorithm[]) => {
        setAvailable(data.filter((a) => !a.alreadyImported));
      })
      .catch((e: unknown) => console.error("Failed to fetch algorithm catalog", e))
      .finally(() => setLoading(false));
  }, [show, api.org]);

  const toggleSelect = useCallback((execClass: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(execClass)) {
        next.delete(execClass);
      } else {
        next.add(execClass);
      }
      return next;
    });
  }, []);

  const toggleAll = useCallback(() => {
    setSelected((prev) => {
      if (prev.size === available.length) {
        return new Set();
      }
      return new Set(available.map((a) => a.execClass));
    });
  }, [available]);

  const importSelected = useCallback(() => {
    if (selected.size === 0) return;
    setImporting(true);
    fetch("/odcsapi/algorithmcatalog", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-ORGANIZATION-ID": api.org,
      },
      body: JSON.stringify([...selected]),
    })
      .then((res) => {
        if (!res.ok) throw new Error(`Import failed: ${res.status}`);
        onImported();
        onHide();
      })
      .catch((e: unknown) => console.error("Failed to import algorithms", e))
      .finally(() => setImporting(false));
  }, [selected, api.org, onImported, onHide]);

  return (
    <Modal show={show} onHide={onHide} centered size="xl">
      <Modal.Header closeButton>
        <Modal.Title>{t("algorithms:check_new.title")}</Modal.Title>
      </Modal.Header>
      <Modal.Body style={{ maxHeight: "60vh", overflowY: "auto" }}>
        {loading ? (
          <div className="text-center p-4">
            <Spinner animation="border" />
          </div>
        ) : available.length === 0 ? (
          <p>{t("algorithms:check_new.none_found")}</p>
        ) : (
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
        <Button variant="secondary" onClick={onHide}>
          {t("algorithms:check_new.close")}
        </Button>
        <Button
          variant="primary"
          onClick={importSelected}
          disabled={selected.size === 0 || importing}
        >
          {importing ? (
            <Spinner animation="border" size="sm" />
          ) : (
            t("algorithms:check_new.import_selected", {
              count: selected.size,
            })
          )}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};
