import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { Button, Form, Modal, Spinner, Table } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiAlgorithm, ApiAlgorithmRef } from "opendcs-api";
import { useAlgorithmsQuery } from "../../../queries/algorithms";

interface Props {
  show: boolean;
  onHide: () => void;
  onSelect: (algo: ApiAlgorithmRef) => void | Promise<void>;
  getAlgorithm?: (algorithmId: number) => Promise<ApiAlgorithm>;
}

export const AlgorithmSelectModal: React.FC<Props> = ({
  show,
  onHide,
  onSelect,
  getAlgorithm,
}) => {
  const [t] = useTranslation(["computations", "translation"]);
  const { data: algorithms = [], isFetching } = useAlgorithmsQuery();
  const [filter, setFilter] = useState("");
  const [selected, setSelected] = useState<ApiAlgorithmRef | null>(null);
  const [fullDescription, setFullDescription] = useState<string | null>(null);
  const [descLoading, setDescLoading] = useState(false);
  const [selecting, setSelecting] = useState(false);

  // Reset modal-local state when reopened so the user sees a fresh picker.
  useEffect(() => {
    if (!show) return;
    setSelected(null);
    setFullDescription(null);
    setFilter("");
    setSelecting(false);
  }, [show]);

  const handleRowClick = useCallback(
    async (algo: ApiAlgorithmRef) => {
      setSelected(algo);
      setFullDescription(null);
      if (!getAlgorithm || !algo.algorithmId) return;
      setDescLoading(true);
      try {
        const full = await getAlgorithm(algo.algorithmId);
        setFullDescription(full.description ?? null);
      } catch (e) {
        console.warn("Failed to fetch full algorithm description", e);
      } finally {
        setDescLoading(false);
      }
    },
    [getAlgorithm],
  );

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
  const selectedDescription =
    selected &&
    (fullDescription ??
      selected.description ??
      t("computations:editor.select_algorithm_no_description"));

  const handleSelect = useCallback(async () => {
    if (selected) {
      setSelecting(true);
      try {
        await onSelect(selected);
        onHide();
      } finally {
        setSelecting(false);
      }
    }
  }, [selected, onSelect, onHide]);

  let algorithmListContent: ReactNode;
  if (isFetching && algorithms.length === 0) {
    algorithmListContent = (
      <div className="text-center p-4">
        <Spinner animation="border" />
      </div>
    );
  } else if (filtered.length === 0) {
    algorithmListContent = <p>{t("computations:editor.select_algorithm_none")}</p>;
  } else {
    algorithmListContent = (
      <div style={{ maxHeight: "45vh", overflowY: "auto" }}>
        <Table striped bordered hover>
          <thead>
            <tr>
              <th>{t("computations:header.Id")}</th>
              <th>{t("computations:editor.name")}</th>
              <th>{t("computations:editor.select_algorithm_exec_class")}</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((algo) => (
              <tr
                key={algo.algorithmId}
                onClick={() => handleRowClick(algo)}
                style={{ cursor: "pointer" }}
                className={
                  selected?.algorithmId === algo.algorithmId ? "table-primary" : ""
                }
              >
                <td>{algo.algorithmId}</td>
                <td>{algo.algorithmName}</td>
                <td>{algo.execClass}</td>
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
        <Modal.Title>{t("computations:editor.select_algorithm_title")}</Modal.Title>
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
        {algorithmListContent}
        {selected && (
          <div
            className="mt-3 p-3 border rounded"
            style={{
              backgroundColor: "var(--bs-body-bg)",
              color: "var(--bs-body-color)",
            }}
          >
            <strong>{selected.algorithmName}</strong>
            <div
              className="mt-2"
              style={{
                color: "var(--bs-secondary-color)",
                whiteSpace: "pre-wrap",
              }}
            >
              {descLoading ? (
                <Spinner animation="border" size="sm" />
              ) : (
                selectedDescription
              )}
            </div>
          </div>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          {t("translation:cancel")}
        </Button>
        <Button
          variant="primary"
          onClick={handleSelect}
          disabled={selected === null || selecting}
        >
          {selecting ? (
            <Spinner animation="border" size="sm" />
          ) : (
            t("translation:select")
          )}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};
