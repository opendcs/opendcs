import { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Form, Modal, Spinner, Table } from "react-bootstrap";
import {
  RESTAlgorithmMethodsApi,
  type ApiAlgorithm,
  type ApiAlgorithmRef,
} from "opendcs-api";
import { useApi } from "../../../contexts/app/ApiContext";

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
  const api = useApi();
  const algorithmApi = useMemo(() => new RESTAlgorithmMethodsApi(api.conf), [api.conf]);
  const [algorithms, setAlgorithms] = useState<ApiAlgorithmRef[]>([]);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState("");
  const [selected, setSelected] = useState<ApiAlgorithmRef | null>(null);
  const [fullDescription, setFullDescription] = useState<string | null>(null);
  const [descLoading, setDescLoading] = useState(false);
  const [selecting, setSelecting] = useState(false);

  useEffect(() => {
    if (!show) return;
    let cancelled = false;
    setLoading(true);
    setSelected(null);
    setFullDescription(null);
    setFilter("");
    setSelecting(false);
    algorithmApi
      .getalgorithmrefs(api.org)
      .then((refs) => {
        if (!cancelled) setAlgorithms(refs);
      })
      .catch((e: unknown) => console.error("Failed to fetch algorithm refs", e))
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [show, api.org, algorithmApi]);

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

  return (
    <Modal show={show} onHide={onHide} centered size="xl">
      <Modal.Header closeButton>
        <Modal.Title>Select Algorithm</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Form.Control
          type="search"
          placeholder="Filter algorithms"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="mb-3"
          aria-label="Filter algorithms"
        />
        {loading ? (
          <div className="text-center p-4">
            <Spinner animation="border" />
          </div>
        ) : filtered.length === 0 ? (
          <p>No algorithms found.</p>
        ) : (
          <div style={{ maxHeight: "45vh", overflowY: "auto" }}>
            <Table striped bordered hover>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Exec Class</th>
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
        )}
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
                (fullDescription ?? selected.description ?? "No description available.")
              )}
            </div>
          </div>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          Cancel
        </Button>
        <Button
          variant="primary"
          onClick={handleSelect}
          disabled={selected === null || selecting}
        >
          {selecting ? <Spinner animation="border" size="sm" /> : "Select"}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};
