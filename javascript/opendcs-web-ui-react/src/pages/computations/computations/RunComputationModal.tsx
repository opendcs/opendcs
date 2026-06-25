import { useCallback, useMemo, useRef, useState } from "react";
import {
  Alert,
  Button,
  Col,
  Form,
  FormGroup,
  Modal,
  Row,
  Spinner,
  Table,
} from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { PlayFill, StopFill } from "react-bootstrap-icons";
import { HttpMethod, TimeSeriesMethodsApi, type ApiTimeSeriesData } from "opendcs-api";
import { useApi } from "../../../contexts/app/ApiContext";

interface TsIdRef {
  uniqueString?: string;
  key?: number;
  storageUnits?: string;
}

interface CompResults {
  tsIds: TsIdRef[];
  startTime: string;
  endTime: string;
}

type RunStatus = "idle" | "running" | "fetching" | "success" | "error";

interface Props {
  show: boolean;
  computationId: number | undefined;
  computationName: string | undefined;
  onHide: () => void;
}

/** Format a Date for a datetime-local input (no seconds). */
const toDatetimeLocal = (d: Date): string => {
  const pad = (n: number) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}`
  );
};

const defaultStart = (): string => {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return toDatetimeLocal(d);
};

const defaultEnd = (): string => toDatetimeLocal(new Date());

/** Build a sorted list of unique timestamps across all series. */
const pivotTimes = (tsData: ApiTimeSeriesData[]): Date[] => {
  const seen = new Set<number>();
  tsData.forEach((ts) =>
    ts.values?.forEach((v) => {
      if (v.sampleTime) seen.add(v.sampleTime.getTime());
    }),
  );
  return Array.from(seen)
    .sort((a, b) => a - b)
    .map((ms) => new Date(ms));
};

/** Look up a value for a given series at a given timestamp. */
const lookupValue = (ts: ApiTimeSeriesData, time: Date): string => {
  const v = ts.values?.find((v) => v.sampleTime?.getTime() === time.getTime());
  return v?.value !== undefined ? String(v.value) : "–";
};

export const RunComputationModal: React.FC<Props> = ({
  show,
  computationId,
  computationName,
  onHide,
}) => {
  const { t } = useTranslation(["computations", "translation"]);
  const { conf, org } = useApi();
  const abortRef = useRef<AbortController | null>(null);
  const tsApi = useMemo(() => new TimeSeriesMethodsApi(conf), [conf]);

  const [startValue, setStartValue] = useState(defaultStart);
  const [endValue, setEndValue] = useState(defaultEnd);
  const [status, setStatus] = useState<RunStatus>("idle");
  const [logs, setLogs] = useState<string[]>([]);
  const [results, setResults] = useState<CompResults | null>(null);
  const [tsData, setTsData] = useState<ApiTimeSeriesData[]>([]);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const logsEndRef = useRef<HTMLDivElement>(null);

  const appendLog = useCallback((msg: string) => {
    setLogs((prev) => [...prev, msg]);
    requestAnimationFrame(() => {
      logsEndRef.current?.scrollIntoView({ behavior: "smooth" });
    });
  }, []);

  const handleRun = useCallback(async () => {
    if (!computationId) return;

    const startIso = new Date(startValue).toISOString();
    const endIso = new Date(endValue).toISOString();

    setStatus("running");
    setLogs([]);
    setResults(null);
    setTsData([]);
    setErrorMsg(null);

    const abort = new AbortController();
    abortRef.current = abort;

    const ctx = conf.baseServer.makeRequestContext("/runcomputation", HttpMethod.GET);
    ctx.setQueryParam("computationid", String(computationId));
    ctx.setQueryParam("start", startIso);
    ctx.setQueryParam("end", endIso);
    const url = ctx.getUrl();

    let parsedResults: CompResults | null = null;
    let hadError = false;

    try {
      const response = await fetch(url, {
        method: "GET",
        credentials: "include",
        headers: { Accept: "text/event-stream", "X-ORGANIZATION-ID": org },
        signal: abort.signal,
      });

      if (!response.ok) {
        const text = await response.text().catch(() => response.statusText);
        throw new Error(`HTTP ${response.status}: ${text}`);
      }
      if (!response.body) throw new Error("No response body");

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";
      let currentEvent = "";
      let currentData = "";

      const dispatch = () => {
        if (!currentData) return;
        if (currentEvent === "computation-status") {
          appendLog(currentData);
        } else if (currentEvent === "Results") {
          try {
            parsedResults = JSON.parse(currentData) as CompResults;
            setResults(parsedResults);
          } catch {
            appendLog(`Results: ${currentData}`);
          }
        } else if (currentEvent === "ERROR") {
          hadError = true;
          appendLog(`Error: ${currentData}`);
          setStatus("error");
          setErrorMsg(currentData);
        } else if (currentData) {
          appendLog(currentData);
        }
        currentEvent = "";
        currentData = "";
      };

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() ?? "";
        for (const line of lines) {
          if (line === "") {
            dispatch();
          } else if (line.startsWith("event:")) {
            currentEvent = line.slice(6).trim();
          } else if (line.startsWith("data:")) {
            const part = line.slice(5).trim();
            currentData = currentData ? `${currentData}\n${part}` : part;
          }
        }
      }
      if (buffer) {
        const line = buffer.trim();
        if (line.startsWith("data:")) currentData = line.slice(5).trim();
        dispatch();
      }

      if (hadError) return;

      // Fetch computed values for each output time series
      if (parsedResults) {
        const validIds = (parsedResults as CompResults).tsIds.filter(
          (id) => id.key !== undefined && id.key > 0,
        );
        if (validIds.length > 0) {
          setStatus("fetching");
          try {
            const fetched = await Promise.all(
              validIds.map((id) =>
                tsApi.getTimeSeriesData(
                  org,
                  id.key!,
                  (parsedResults as CompResults).startTime,
                  (parsedResults as CompResults).endTime,
                ),
              ),
            );
            setTsData(fetched);
          } catch (fetchErr) {
            appendLog(
              `${t("computations:run.fetch_warning")}: ${(fetchErr as Error).message}`,
            );
          }
        }
      }

      setStatus("success");
    } catch (err) {
      if ((err as Error).name === "AbortError") {
        appendLog(t("computations:run.aborted"));
        setStatus("idle");
      } else {
        const msg = (err as Error).message ?? String(err);
        setErrorMsg(msg);
        setStatus("error");
      }
    } finally {
      abortRef.current = null;
    }
  }, [computationId, startValue, endValue, conf, org, appendLog, t, tsApi]);

  const handleStop = useCallback(() => {
    abortRef.current?.abort();
  }, []);

  const handleHide = useCallback(() => {
    abortRef.current?.abort();
    setStatus("idle");
    setLogs([]);
    setResults(null);
    setTsData([]);
    setErrorMsg(null);
    setStartValue(defaultStart());
    setEndValue(defaultEnd());
    onHide();
  }, [onHide]);

  const running = status === "running" || status === "fetching";
  const times = useMemo(() => pivotTimes(tsData), [tsData]);
  const hasValues = tsData.some((ts) => (ts.values?.length ?? 0) > 0);

  return (
    <Modal show={show} onHide={handleHide} size={hasValues ? "xl" : "lg"}>
      <Modal.Header closeButton>
        <Modal.Title>
          {t("computations:run.title", { name: computationName ?? computationId })}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Row className="mb-3 g-3">
          <Col sm={6}>
            <FormGroup>
              <Form.Label htmlFor="run-start">{t("computations:run.start")}</Form.Label>
              <Form.Control
                type="datetime-local"
                id="run-start"
                value={startValue}
                disabled={running}
                onChange={(e) => setStartValue(e.target.value)}
              />
            </FormGroup>
          </Col>
          <Col sm={6}>
            <FormGroup>
              <Form.Label htmlFor="run-end">{t("computations:run.end")}</Form.Label>
              <Form.Control
                type="datetime-local"
                id="run-end"
                value={endValue}
                disabled={running}
                onChange={(e) => setEndValue(e.target.value)}
              />
            </FormGroup>
          </Col>
        </Row>

        {logs.length > 0 && (
          <div
            className="border rounded p-2 mb-3 bg-dark text-light font-monospace"
            style={{ maxHeight: "10rem", overflowY: "auto", fontSize: "0.8rem" }}
            aria-label={t("computations:run.log_label")}
            aria-live="polite"
          >
            {logs.map((line, i) => (
              <div key={i}>{line}</div>
            ))}
            <div ref={logsEndRef} />
          </div>
        )}

        {status === "error" && errorMsg && <Alert variant="danger">{errorMsg}</Alert>}

        {status === "fetching" && (
          <div className="d-flex align-items-center gap-2 text-muted mb-3">
            <Spinner size="sm" animation="border" />
            <span>{t("computations:run.fetching_data")}</span>
          </div>
        )}

        {results && (
          <div className="mt-2">
            <strong>{t("computations:run.results_title")}</strong>
            {results.tsIds.length === 0 ? (
              <p className="text-muted mt-1">{t("computations:run.no_outputs")}</p>
            ) : hasValues ? (
              <div className="mt-2" style={{ maxHeight: "22rem", overflowY: "auto" }}>
                <Table
                  size="sm"
                  bordered
                  hover
                  className="font-monospace mb-0"
                  style={{ fontSize: "0.8rem" }}
                >
                  <thead className="table-dark sticky-top">
                    <tr>
                      <th>{t("translation:date_time")}</th>
                      {tsData.map((ts, i) => (
                        <th key={i}>
                          {ts.tsid?.uniqueString ??
                            results.tsIds[i]?.uniqueString ??
                            t("computations:run.unknown_ts")}
                          {ts.tsid?.storageUnits ? ` (${ts.tsid.storageUnits})` : ""}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {times.map((time, ri) => (
                      <tr key={ri}>
                        <td className="text-nowrap">{time.toLocaleString()}</td>
                        {tsData.map((ts, ci) => (
                          <td key={ci}>{lookupValue(ts, time)}</td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </div>
            ) : status === "success" ? (
              <p
                className="text-muted mt-1 font-monospace"
                style={{ fontSize: "0.85rem" }}
              >
                {results.tsIds
                  .map((id) => id.uniqueString ?? t("computations:run.unknown_ts"))
                  .join(", ")}
                {" — "}
                {t("computations:run.no_data")}
              </p>
            ) : null}
          </div>
        )}
      </Modal.Body>
      <Modal.Footer>
        {running && (
          <Spinner
            size="sm"
            animation="border"
            className="me-2"
            aria-label={
              status === "fetching"
                ? t("computations:run.fetching_data")
                : t("computations:run.running")
            }
          />
        )}
        <Button variant="secondary" onClick={handleHide}>
          {t("translation:cancel")}
        </Button>
        {running ? (
          <Button
            variant="warning"
            onClick={handleStop}
            aria-label={t("computations:run.stop")}
            disabled={status === "fetching"}
          >
            <StopFill /> {t("computations:run.stop")}
          </Button>
        ) : (
          <Button
            variant="success"
            onClick={handleRun}
            disabled={!computationId || !startValue || !endValue}
            aria-label={t("computations:run.run")}
          >
            <PlayFill /> {t("computations:run.run")}
          </Button>
        )}
      </Modal.Footer>
    </Modal>
  );
};

export default RunComputationModal;
