import { useCallback, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Alert,
  Button,
  Col,
  Form,
  FormCheck,
  FormGroup,
  Modal,
  Row,
  Spinner,
  Table,
} from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { PlayFill, StopFill } from "react-bootstrap-icons";
import {
  HttpMethod,
  TimeSeriesMethodsApi,
  type ApiTimeSeriesData,
  type ApiTimeSeriesIdentifier,
} from "opendcs-api";
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

interface RunRun {
  tsid: ApiTimeSeriesIdentifier;
  results: CompResults | null;
  tsData: ApiTimeSeriesData[];
}

interface Props {
  show: boolean;
  computationId: number | undefined;
  computationName: string | undefined;
  groupId?: number;
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
  return v?.value === undefined ? "–" : String(v.value);
};

interface SseCallbacks {
  onLog: (msg: string) => void;
  onResults: (r: CompResults) => void;
  onError: (msg: string) => void;
}

interface LogEntry {
  id: number;
  text: string;
  header?: boolean;
}

const appendSsePart = (current: string, part: string): string =>
  current ? `${current}\n${part}` : part;

const dispatchSseEvent = (
  event: string,
  data: string,
  callbacks: SseCallbacks,
): boolean => {
  if (!data) return false;
  if (event === "computation-status") {
    callbacks.onLog(data);
  } else if (event === "Results") {
    try {
      callbacks.onResults(JSON.parse(data) as CompResults);
    } catch {
      callbacks.onLog(`Results: ${data}`);
    }
  } else if (event === "ERROR") {
    callbacks.onLog(`Error: ${data}`);
    callbacks.onError(data);
    return true;
  } else {
    callbacks.onLog(data);
  }
  return false;
};

/**
 * Extract a short, human-readable message from a non-OK response. The API's
 * error bodies are JSON `{ message }`, but a container/proxy-level failure
 * (e.g. a Tomcat error page) can return raw HTML instead — fall back to the
 * status text rather than dumping that markup into the UI.
 */
const readErrorMessage = async (response: Response): Promise<string> => {
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) return body.message;
    } catch {
      // fall through to statusText below
    }
  }
  return response.statusText || `status ${response.status}`;
};

/** Read a text/event-stream response body and call callbacks for each event. */
const readSseStream = async (
  body: ReadableStream<Uint8Array>,
  callbacks: SseCallbacks,
): Promise<boolean> => {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let currentEvent = "";
  let currentData = "";
  let hadError = false;

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";
    for (const line of lines) {
      if (line === "") {
        hadError = dispatchSseEvent(currentEvent, currentData, callbacks) || hadError;
        currentEvent = "";
        currentData = "";
      } else if (line.startsWith("event:")) {
        currentEvent = line.slice(6).trim();
      } else if (line.startsWith("data:")) {
        currentData = appendSsePart(currentData, line.slice(5).trim());
      }
    }
  }
  const finalLine = buffer.trim();
  if (finalLine.startsWith("data:")) currentData = finalLine.slice(5).trim();
  hadError = dispatchSseEvent(currentEvent, currentData, callbacks) || hadError;
  return hadError;
};

interface ResultsSectionProps {
  runs: RunRun[];
  isGroup: boolean;
  status: RunStatus;
  t: (key: string) => string;
}

const ResultsSection: React.FC<ResultsSectionProps> = ({
  runs,
  isGroup,
  status,
  t,
}) => {
  const sections = runs.filter((r) => r.results !== null);

  if (sections.length === 0) return null;

  return (
    <>
      {sections.map((run) => {
        const label = run.tsid.uniqueString ?? t("computations:run.unknown_ts");
        const times = pivotTimes(run.tsData);
        const hasValues = run.tsData.some((ts) => (ts.values?.length ?? 0) > 0);

        if (run.results!.tsIds.length === 0) {
          return (
            <p key={label} className="text-muted mt-1">
              {isGroup ? (
                <>
                  <strong>{label}</strong>:{" "}
                </>
              ) : null}
              {t("computations:run.no_outputs")}
            </p>
          );
        }
        if (hasValues) {
          return (
            <div key={label} className="mt-2">
              {isGroup && (
                <div
                  className="fw-semibold mb-1 font-monospace"
                  style={{ fontSize: "0.85rem" }}
                >
                  {label}
                </div>
              )}
              <div style={{ maxHeight: "18rem", overflowY: "auto" }}>
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
                      {run.tsData.map((ts, i) => (
                        <th key={ts.tsid?.uniqueString ?? ts.tsid?.key}>
                          {ts.tsid?.uniqueString ??
                            run.results!.tsIds[i]?.uniqueString ??
                            t("computations:run.unknown_ts")}
                          {ts.tsid?.storageUnits ? ` (${ts.tsid.storageUnits})` : ""}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {times.map((time) => (
                      <tr key={time.getTime()}>
                        <td className="text-nowrap">{time.toLocaleString()}</td>
                        {run.tsData.map((ts) => (
                          <td key={ts.tsid?.uniqueString ?? ts.tsid?.key}>
                            {lookupValue(ts, time)}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </div>
            </div>
          );
        }
        if (status === "success") {
          return (
            <p
              key={label}
              className="text-muted mt-1 font-monospace"
              style={{ fontSize: "0.85rem" }}
            >
              {isGroup && (
                <>
                  <strong>{label}</strong>:{" "}
                </>
              )}
              {run
                .results!.tsIds.map(
                  (id) => id.uniqueString ?? t("computations:run.unknown_ts"),
                )
                .join(", ")}
              {" — "}
              {t("computations:run.no_data")}
            </p>
          );
        }
        return null;
      })}
    </>
  );
};

export const RunComputationModal: React.FC<Props> = ({
  show,
  computationId,
  computationName,
  groupId,
  onHide,
}) => {
  const { t } = useTranslation(["computations", "translation"]);
  const { conf, org } = useApi();
  const abortRef = useRef<AbortController | null>(null);
  const logCounterRef = useRef(0);
  const tsApi = useMemo(() => new TimeSeriesMethodsApi(conf), [conf]);

  const [startValue, setStartValue] = useState(defaultStart);
  const [endValue, setEndValue] = useState(defaultEnd);
  const [status, setStatus] = useState<RunStatus>("idle");
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [runs, setRuns] = useState<RunRun[]>([]);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const logsEndRef = useRef<HTMLDivElement>(null);

  const isGroup = groupId !== undefined;

  // Fetch expanded group members via React Query (avoid useEffect+setState pattern).
  // Use an "excluded keys" set so all entries are selected by default without any
  // initialization effect — the user starts with everything checked and unchecks as desired.
  const {
    data: groupTsIds = [],
    isLoading: loadingGroup,
    error: groupQueryError,
  } = useQuery<ApiTimeSeriesIdentifier[]>({
    queryKey: ["expandgroup", groupId, computationId, org],
    queryFn: async () => {
      const ctx = conf.baseServer.makeRequestContext("/expandgroup", HttpMethod.GET);
      ctx.setQueryParam("groupid", String(groupId));
      // Filters the group's raw members down to ones that actually resolve against
      // this computation's first input parm (datatype/interval can otherwise differ
      // from what the group's site/datatype expansion alone implies) — mirrors the
      // desktop client's Run Computation dialog.
      if (computationId !== undefined) {
        ctx.setQueryParam("computationid", String(computationId));
      }
      const url = ctx.getUrl();
      const res = await fetch(url, {
        method: "GET",
        credentials: "include",
        headers: { Accept: "application/json", "X-ORGANIZATION-ID": org },
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return res.json() as Promise<ApiTimeSeriesIdentifier[]>;
    },
    enabled: show && !!groupId,
    retry: 0,
    staleTime: 0,
  });

  const groupError = groupQueryError ? (groupQueryError as Error).message : null;

  // Track which keys the user has explicitly excluded; everything else is selected.
  const [excludedKeys, setExcludedKeys] = useState<Set<number>>(new Set());

  const selectedKeys = useMemo(
    () =>
      new Set(
        groupTsIds
          .map((ts) => ts.key)
          .filter((k): k is number => k !== undefined && !excludedKeys.has(k)),
      ),
    [groupTsIds, excludedKeys],
  );

  const appendLog = useCallback((msg: string, header = false) => {
    const id = logCounterRef.current++;
    setLogs((prev) => [...prev, { id, text: msg, header }]);
    requestAnimationFrame(() => {
      logsEndRef.current?.scrollIntoView({ behavior: "smooth" });
    });
  }, []);

  /** Fetch the computed output time series values after a successful run. */
  const fetchTsData = useCallback(
    async (results: CompResults): Promise<ApiTimeSeriesData[]> => {
      const validIds = results.tsIds.filter((id) => id.key !== undefined && id.key > 0);
      if (validIds.length === 0) return [];
      setStatus("fetching");
      try {
        return await Promise.all(
          validIds.map((id) =>
            tsApi.getTimeSeriesData(org, id.key!, results.startTime, results.endTime),
          ),
        );
      } catch (fetchErr) {
        appendLog(
          `${t("computations:run.fetch_warning")}: ${(fetchErr as Error).message}`,
        );
        return [];
      }
    },
    [org, tsApi, appendLog, t],
  );

  /** Call the SSE runcomputation endpoint and return parsed results. */
  const streamComputation = useCallback(
    async (
      url: string,
      abort: AbortController,
    ): Promise<{ results: CompResults | null; hadError: boolean }> => {
      const response = await fetch(url, {
        method: "GET",
        credentials: "include",
        headers: { Accept: "text/event-stream", "X-ORGANIZATION-ID": org },
        signal: abort.signal,
      });
      if (!response.ok) {
        const message = await readErrorMessage(response);
        throw new Error(`HTTP ${response.status}: ${message}`);
      }
      if (!response.body) throw new Error("No response body");
      let parsedResults: CompResults | null = null;
      const hadError = await readSseStream(response.body, {
        onLog: appendLog,
        onResults: (r) => {
          parsedResults = r;
        },
        onError: (msg) => {
          setStatus("error");
          setErrorMsg(msg);
        },
      });
      return { results: parsedResults, hadError };
    },
    [org, appendLog],
  );

  /** Run the computation for one input TS ID (or undefined for non-group). */
  const runOne = useCallback(
    async (
      tsid: ApiTimeSeriesIdentifier | undefined,
      startIso: string,
      endIso: string,
      abort: AbortController,
    ): Promise<RunRun | null> => {
      if (!computationId) return null;
      const ctx = conf.baseServer.makeRequestContext("/runcomputation", HttpMethod.GET);
      ctx.setQueryParam("computationid", String(computationId));
      ctx.setQueryParam("start", startIso);
      ctx.setQueryParam("end", endIso);
      if (tsid?.key !== undefined) ctx.setQueryParam("tsid", String(tsid.key));
      const { results, hadError } = await streamComputation(ctx.getUrl(), abort);
      // null signals an SSE ERROR event — caller should not set success status
      if (hadError) return null;
      if (!results) return { tsid: tsid ?? {}, results, tsData: [] };
      const tsData = await fetchTsData(results);
      return { tsid: tsid ?? {}, results, tsData };
    },
    [computationId, conf, streamComputation, fetchTsData],
  );

  /** Sequential loop over selected group TS IDs. Returns true if an SSE ERROR was received. */
  const runGroupSequential = useCallback(
    async (
      startIso: string,
      endIso: string,
      abort: AbortController,
    ): Promise<boolean> => {
      const toRun = groupTsIds.filter(
        (ts) => ts.key !== undefined && selectedKeys.has(ts.key),
      );
      const accumulated: RunRun[] = [];
      for (const tsid of toRun) {
        if (abort.signal.aborted) break;
        appendLog(
          `--- ${t("computations:run.group_running_for", { tsid: tsid.uniqueString ?? tsid.key })} ---`,
          true,
        );
        setStatus("running");
        const result = await runOne(tsid, startIso, endIso, abort);
        if (result === null) return true;
        accumulated.push(result);
        setRuns([...accumulated]);
      }
      return false;
    },
    [groupTsIds, selectedKeys, appendLog, t, runOne],
  );

  const handleRun = useCallback(async () => {
    if (!computationId) return;

    const startIso = new Date(startValue).toISOString();
    const endIso = new Date(endValue).toISOString();

    setStatus("running");
    setLogs([]);
    setRuns([]);
    setErrorMsg(null);

    const abort = new AbortController();
    abortRef.current = abort;

    try {
      let hadSseError = false;
      if (isGroup) {
        hadSseError = await runGroupSequential(startIso, endIso, abort);
      } else {
        const result = await runOne(undefined, startIso, endIso, abort);
        if (result === null) {
          hadSseError = true;
        } else {
          setRuns([result]);
        }
      }
      if (!abort.signal.aborted && !hadSseError) setStatus("success");
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
  }, [
    computationId,
    startValue,
    endValue,
    isGroup,
    appendLog,
    t,
    runOne,
    runGroupSequential,
  ]);

  const handleStop = useCallback(() => {
    abortRef.current?.abort();
  }, []);

  const handleHide = useCallback(() => {
    abortRef.current?.abort();
    setStatus("idle");
    setLogs([]);
    setRuns([]);
    setErrorMsg(null);
    setStartValue(defaultStart());
    setEndValue(defaultEnd());
    setExcludedKeys(new Set());
    onHide();
  }, [onHide]);

  const toggleKey = useCallback((key: number) => {
    setExcludedKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  }, []);

  const selectAll = useCallback(() => {
    setExcludedKeys(new Set());
  }, []);

  const selectNone = useCallback(() => {
    setExcludedKeys(
      new Set(
        groupTsIds.map((ts) => ts.key).filter((k): k is number => k !== undefined),
      ),
    );
  }, [groupTsIds]);

  const running = status === "running" || status === "fetching";
  const hasAnyResults = runs.length > 0;
  const hasValues = runs.some((r) =>
    r.tsData.some((ts) => (ts.values?.length ?? 0) > 0),
  );
  const canRun =
    !!computationId &&
    !!startValue &&
    !!endValue &&
    (!isGroup || selectedKeys.size > 0);

  return (
    <Modal show={show} onHide={handleHide} size={hasValues ? "xl" : "lg"}>
      <Modal.Header closeButton>
        <Modal.Title>
          {t("computations:run.title", { name: computationName ?? computationId })}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {/* Group input selection */}
        {isGroup && (
          <div className="mb-3">
            <div className="d-flex align-items-center justify-content-between mb-1">
              <Form.Label className="mb-0 fw-semibold">
                {t("computations:run.group_select_label")}
              </Form.Label>
              {!loadingGroup && groupTsIds.length > 0 && (
                <div className="d-flex gap-2">
                  <Button variant="link" size="sm" className="p-0" onClick={selectAll}>
                    {t("computations:run.group_select_all")}
                  </Button>
                  <Button variant="link" size="sm" className="p-0" onClick={selectNone}>
                    {t("computations:run.group_select_none")}
                  </Button>
                </div>
              )}
            </div>
            {loadingGroup && (
              <div className="d-flex align-items-center gap-2 text-muted">
                <Spinner size="sm" animation="border" />
                <span>{t("computations:run.group_loading")}</span>
              </div>
            )}
            {groupError && (
              <Alert variant="warning">
                {t("computations:run.group_load_error")}: {groupError}
              </Alert>
            )}
            {!loadingGroup && groupTsIds.length === 0 && !groupError && (
              <p className="text-muted mb-0" style={{ fontSize: "0.875rem" }}>
                {t("computations:run.group_empty")}
              </p>
            )}
            {!loadingGroup && groupTsIds.length > 0 && (
              <div
                className="border rounded p-2"
                style={{ maxHeight: "10rem", overflowY: "auto" }}
              >
                {groupTsIds.map((ts) => {
                  const key = ts.key ?? -1;
                  return (
                    <FormCheck
                      key={key}
                      id={`group-ts-${key}`}
                      type="checkbox"
                      label={ts.uniqueString ?? String(key)}
                      checked={selectedKeys.has(key)}
                      onChange={() => toggleKey(key)}
                      disabled={running}
                      className="font-monospace"
                      style={{ fontSize: "0.85rem" }}
                    />
                  );
                })}
              </div>
            )}
          </div>
        )}

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
            {logs.map((entry) => (
              <div
                key={entry.id}
                className={entry.header ? "text-warning fw-bold mt-1" : undefined}
              >
                {entry.text}
              </div>
            ))}
            <div ref={logsEndRef} />
          </div>
        )}

        {status === "error" && errorMsg && (
          <Alert
            variant="danger"
            style={{
              maxHeight: "16rem",
              overflowY: "auto",
              overflowWrap: "anywhere",
              whiteSpace: "pre-wrap",
            }}
          >
            {errorMsg}
          </Alert>
        )}

        {status === "fetching" && (
          <div className="d-flex align-items-center gap-2 text-muted mb-3">
            <Spinner size="sm" animation="border" />
            <span>{t("computations:run.fetching_data")}</span>
          </div>
        )}

        {hasAnyResults && (
          <div className="mt-2">
            <strong>{t("computations:run.results_title")}</strong>
            <ResultsSection runs={runs} isGroup={isGroup} status={status} t={t} />
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
            disabled={!canRun}
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
