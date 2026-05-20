import { useCallback, useMemo, useState, type ReactNode } from "react";
import { Button, Form, Modal, Spinner, Table } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiAlgorithmRef } from "opendcs-api";
import { useAlgorithmsQuery } from "../../../queries/algorithms";
import { QueryErrorBoundary } from "../../../components/QueryErrorBoundary";

interface Props {
  show: boolean;
  onHide: () => void;
  onAdd: (algorithms: ApiAlgorithmRef[]) => Promise<void> | void;
}

const BulkAddComputationsModalInner: React.FC<Props> = ({ show, onHide, onAdd }) => {
  const [t] = useTranslation(["computations", "translation"]);
  const { data: algorithms = [], isLoading } = useAlgorithmsQuery();
  const [filter, setFilter] = useState("");
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [adding, setAdding] = useState(false);

  const filtered = useMemo(() => {
    const q = filter.trim().toLowerCase();
    if (!q) return algorithms;
    return algorithms.filter(
      (a) =>
        (a.algorithmName ?? "").toLowerCase().includes(q) ||
        (a.execClass ?? "").toLowerCase().includes(q) ||
        (a.description ?? "").toLowerCase().includes(q),
    );
  }, [algorithms, filter]);

  const toggleSelect = useCallback((id: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);

  const toggleAll = useCallback(() => {
    setSelected((prev) => {
      const filteredIds = filtered
        .map((a) => a.algorithmId)
        .filter((id): id is number => id !== undefined);
      const allSelected = filteredIds.every((id) => prev.has(id));
      if (allSelected) {
        const next = new Set(prev);
        filteredIds.forEach((id) => next.delete(id));
        return next;
      }
      const next = new Set(prev);
      filteredIds.forEach((id) => next.add(id));
      return next;
    });
  }, [filtered]);

  const filteredSelectedCount = filtered.filter(
    (a) => a.algorithmId !== undefined && selected.has(a.algorithmId),
  ).length;
  const allFilteredSelected =
    filtered.length > 0 && filteredSelectedCount === filtered.length;

  const handleAdd = useCallback(async () => {
    const selectedAlgos = algorithms.filter(
      (a) => a.algorithmId !== undefined && selected.has(a.algorithmId),
    );
    setAdding(true);
    try {
      await onAdd(selectedAlgos);
    } finally {
      setAdding(false);
      setSelected(new Set());
      setFilter("");
    }
    onHide();
  }, [algorithms, selected, onAdd, onHide]);

  let bodyContent: ReactNode;
  if (isLoading) {
    bodyContent = (
      <div className="text-center p-4">
        <Spinner animation="border" />
      </div>
    );
  } else if (filtered.length === 0) {
    bodyContent = <p>{t("computations:add_from_algorithms.none_found")}</p>;
  } else {
    bodyContent = (
      <div style={{ maxHeight: "50vh", overflowY: "auto" }}>
        <Table striped bordered hover>
          <thead>
            <tr>
              <th style={{ width: "2.5rem" }}>
                <Form.Check
                  type="checkbox"
                  checked={allFilteredSelected}
                  onChange={toggleAll}
                  aria-label={t("computations:add_from_algorithms.select_all")}
                />
              </th>
              <th>{t("computations:header.Id")}</th>
              <th>{t("computations:editor.name")}</th>
              <th>{t("computations:editor.select_algorithm_exec_class")}</th>
              <th>{t("computations:header.Description")}</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((algo) => (
              <tr
                key={algo.algorithmId}
                onClick={() =>
                  algo.algorithmId !== undefined && toggleSelect(algo.algorithmId)
                }
                style={{ cursor: "pointer" }}
                className={
                  algo.algorithmId !== undefined && selected.has(algo.algorithmId)
                    ? "table-primary"
                    : ""
                }
              >
                <td>
                  <Form.Check
                    type="checkbox"
                    checked={
                      algo.algorithmId !== undefined && selected.has(algo.algorithmId)
                    }
                    readOnly
                    aria-label={algo.algorithmName}
                  />
                </td>
                <td>{algo.algorithmId}</td>
                <td>{algo.algorithmName}</td>
                <td>{algo.execClass}</td>
                <td>{algo.description}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      </div>
    );
  }

  return (
    <Modal show={show} onHide={onHide} centered size="xl">
      <Modal.Header closeButton>
        <Modal.Title>{t("computations:add_from_algorithms.title")}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Form.Control
          type="search"
          placeholder={t("computations:editor.select_algorithm_filter")}
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="mb-3"
          aria-label={t("computations:editor.select_algorithm_filter")}
        />
        {bodyContent}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          {t("translation:cancel")}
        </Button>
        <Button
          variant="primary"
          onClick={handleAdd}
          disabled={selected.size === 0 || adding}
        >
          {adding ? (
            <>
              <Spinner animation="border" size="sm" className="me-2" />
              {t("computations:add_from_algorithms.adding")}
            </>
          ) : (
            t("computations:add_from_algorithms.add_selected", {
              count: selected.size,
            })
          )}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export const BulkAddComputationsModal: React.FC<Props> = (props) => (
  <QueryErrorBoundary>
    <BulkAddComputationsModalInner {...props} />
  </QueryErrorBoundary>
);
