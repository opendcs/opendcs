import type {
  ApiTimeSeriesData,
  ApiTimeSeriesIdentifier,
  TimeSeriesMethodsApi,
} from "opendcs-api";

export interface ComputationRunResultsEvent {
  tsIds?: ApiTimeSeriesIdentifier[];
  startTime?: string;
  endTime?: string;
}

export interface ComputationRunResult {
  messages: string[];
  errors: string[];
  results?: ComputationRunResultsEvent;
  series: ApiTimeSeriesData[];
}

interface SseEvent {
  event: string;
  data: string;
}

const runUrl = (computationId: number, start: Date, end: Date): string => {
  const params = new URLSearchParams({
    computationid: String(computationId),
    start: start.toISOString(),
    end: end.toISOString(),
  });
  return `/odcsapi/runcomputation?${params.toString()}`;
};

const dayOfYear = (date: Date): number => {
  const startOfYear = Date.UTC(date.getUTCFullYear(), 0, 1);
  const currentDay = Date.UTC(
    date.getUTCFullYear(),
    date.getUTCMonth(),
    date.getUTCDate(),
  );
  return Math.floor((currentDay - startOfYear) / 86_400_000) + 1;
};

export const formatTimeSeriesRangeDate = (date: Date): string => {
  const pad = (value: number, width = 2) => String(value).padStart(width, "0");
  return `${date.getUTCFullYear()}/${pad(dayOfYear(date), 3)}/${pad(
    date.getUTCHours(),
  )}:${pad(date.getUTCMinutes())}:${pad(date.getUTCSeconds())}`;
};

const parseSseLine = (
  line: string,
  current: { event: string; data: string[] },
  onEvent: (event: SseEvent) => void,
): { event: string; data: string[] } => {
  if (line === "") {
    if (current.data.length > 0) {
      onEvent({ event: current.event, data: current.data.join("\n") });
    }
    return { event: "message", data: [] };
  }

  if (line.startsWith("event:")) {
    return { ...current, event: line.slice("event:".length).trim() };
  }
  if (line.startsWith("data:")) {
    return {
      ...current,
      data: [...current.data, line.slice("data:".length).trimStart()],
    };
  }
  return current;
};

export const runComputationStream = async (
  org: string,
  computationId: number,
  start: Date,
  end: Date,
): Promise<ComputationRunResult> => {
  const messages: string[] = [];
  const errors: string[] = [];
  let results: ComputationRunResultsEvent | undefined;

  const response = await fetch(runUrl(computationId, start, end), {
    headers: {
      Accept: "text/event-stream",
      "X-ORGANIZATION-ID": org,
    },
    credentials: "include",
  });

  if (!response.ok) {
    const body = await response.text().catch(() => "");
    throw new Error(body || `Run failed with HTTP ${response.status}`);
  }
  if (!response.body) {
    throw new Error("Run failed: response stream was empty.");
  }

  const onEvent = (event: SseEvent) => {
    if (event.event === "Results") {
      results = JSON.parse(event.data) as ComputationRunResultsEvent;
      return;
    }
    if (event.event === "ERROR") {
      errors.push(event.data);
      return;
    }
    if (event.data.trim().length > 0) {
      messages.push(event.data);
    }
  };

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let current = { event: "message", data: [] as string[] };

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split(/\r?\n/);
    buffer = lines.pop() ?? "";
    lines.forEach((line) => {
      current = parseSseLine(line, current, onEvent);
    });
  }

  buffer += decoder.decode();
  if (buffer.length > 0) {
    current = parseSseLine(buffer, current, onEvent);
  }
  if (current.data.length > 0) {
    onEvent({ event: current.event, data: current.data.join("\n") });
  }

  return { messages, errors, results, series: [] };
};

const hasResolvedKey = (tsid: ApiTimeSeriesIdentifier): boolean =>
  tsid.key !== undefined && tsid.key > 0;

const resolveRunResultTsIds = async (
  timeSeriesApi: TimeSeriesMethodsApi,
  org: string,
  tsIds: ApiTimeSeriesIdentifier[],
): Promise<ApiTimeSeriesIdentifier[]> => {
  if (tsIds.every(hasResolvedKey)) {
    return tsIds;
  }

  const refs = await timeSeriesApi.getTimeSeriesRefs(org, false);
  const refsByUniqueString = new Map<string, ApiTimeSeriesIdentifier>();
  refs.forEach((ref) => {
    const uniqueString = ref.uniqueString?.trim();
    if (uniqueString) {
      refsByUniqueString.set(uniqueString, ref);
    }
  });

  return tsIds.map((tsid) => {
    if (hasResolvedKey(tsid)) {
      return tsid;
    }

    const uniqueString = tsid.uniqueString?.trim();
    if (!uniqueString) {
      return tsid;
    }

    const resolved = refsByUniqueString.get(uniqueString);
    if (!resolved || !hasResolvedKey(resolved)) {
      return tsid;
    }

    return {
      ...tsid,
      key: resolved.key,
      storageUnits: tsid.storageUnits ?? resolved.storageUnits,
    };
  });
};

export const loadRunResultTimeSeries = async (
  timeSeriesApi: TimeSeriesMethodsApi,
  org: string,
  runResult: ComputationRunResult,
): Promise<ComputationRunResult> => {
  const originalTsIds = runResult.results?.tsIds ?? [];
  const start = runResult.results?.startTime
    ? formatTimeSeriesRangeDate(new Date(runResult.results.startTime))
    : undefined;
  const end = runResult.results?.endTime
    ? formatTimeSeriesRangeDate(new Date(runResult.results.endTime))
    : undefined;
  const errors = [...runResult.errors];
  let tsIds = originalTsIds;

  try {
    tsIds = await resolveRunResultTsIds(timeSeriesApi, org, originalTsIds);
  } catch (error) {
    errors.push(
      error instanceof Error
        ? error.message
        : "Failed to resolve output time series identifiers.",
    );
  }

  const unresolvedTsIds = tsIds.filter((tsid) => !hasResolvedKey(tsid));
  if (unresolvedTsIds.length > 0) {
    errors.push(
      `Unable to resolve ${unresolvedTsIds.length} output time series key(s).`,
    );
  }

  const settled = await Promise.allSettled(
    tsIds
      .filter(hasResolvedKey)
      .map((tsid) => timeSeriesApi.getTimeSeriesData(org, tsid.key!, start, end)),
  );

  const series: ApiTimeSeriesData[] = [];
  settled.forEach((result) => {
    if (result.status === "fulfilled") {
      series.push(result.value);
    } else {
      errors.push(
        result.reason instanceof Error
          ? result.reason.message
          : "Failed to load output time series data.",
      );
    }
  });

  return {
    ...runResult,
    errors,
    results: runResult.results ? { ...runResult.results, tsIds } : runResult.results,
    series,
  };
};
