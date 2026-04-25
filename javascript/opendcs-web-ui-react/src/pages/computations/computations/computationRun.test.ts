import { afterEach, expect, test, vi } from "vitest";
import {
  formatTimeSeriesRangeDate,
  loadRunResultTimeSeries,
  runComputationStream,
} from "./computationRun";

afterEach(() => {
  vi.restoreAllMocks();
});

test("formatTimeSeriesRangeDate formats UTC Julian dates for tsdata", () => {
  const date = new Date("2026-04-21T19:30:05Z");

  expect(formatTimeSeriesRangeDate(date)).toEqual("2026/111/19:30:05");
});

test("runComputationStream parses status, errors, and results SSE events", async () => {
  const body = [
    "event: computation-status\n",
    "data: Running computation with ID: 42\n\n",
    "event: ERROR\n",
    "data: Missing optional output site\n\n",
    "event: Results\n",
    'data: {"startTime":"2026-04-21T00:00:00Z","endTime":"2026-04-22T00:00:00Z","tsIds":[{"key":7,"uniqueString":"LOC.Flow.Inst.1Hour.0.raw","storageUnits":"cfs"}]}\n\n',
  ].join("");

  const fetchMock = vi.fn(async () => {
    return new Response(body, {
      status: 200,
      headers: { "Content-Type": "text/event-stream" },
    });
  });
  vi.stubGlobal("fetch", fetchMock);

  const result = await runComputationStream(
    "default",
    42,
    new Date("2026-04-21T00:00:00Z"),
    new Date("2026-04-22T00:00:00Z"),
  );

  expect(fetchMock).toHaveBeenCalledWith(
    "/odcsapi/runcomputation?computationid=42&start=2026-04-21T00%3A00%3A00.000Z&end=2026-04-22T00%3A00%3A00.000Z",
    expect.objectContaining({
      credentials: "include",
      headers: expect.objectContaining({
        Accept: "text/event-stream",
        "X-ORGANIZATION-ID": "default",
      }),
    }),
  );
  expect(result.messages).toEqual(["Running computation with ID: 42"]);
  expect(result.errors).toEqual(["Missing optional output site"]);
  expect(result.results?.tsIds?.[0]).toEqual(
    expect.objectContaining({
      key: 7,
      uniqueString: "LOC.Flow.Inst.1Hour.0.raw",
      storageUnits: "cfs",
    }),
  );
});

test("loadRunResultTimeSeries resolves missing output keys by unique string", async () => {
  const getTimeSeriesRefs = vi.fn(async () => [
    {
      key: 13,
      uniqueString: "LOC.Flow.Inst.1Hour.0.raw",
      storageUnits: "cfs",
      active: true,
    },
  ]);
  const getTimeSeriesData = vi.fn(async () => ({
    tsid: { key: 13, uniqueString: "LOC.Flow.Inst.1Hour.0.raw", storageUnits: "cfs" },
    values: [{ sampleTime: new Date("2026-04-21T01:00:00Z"), value: 12.5 }],
  }));

  const result = await loadRunResultTimeSeries(
    {
      getTimeSeriesRefs,
      getTimeSeriesData,
    } as never,
    "default",
    {
      messages: [],
      errors: [],
      results: {
        startTime: "2026-04-21T00:00:00Z",
        endTime: "2026-04-22T00:00:00Z",
        tsIds: [{ key: -1, uniqueString: "LOC.Flow.Inst.1Hour.0.raw" }],
      },
      series: [],
    },
  );

  expect(getTimeSeriesRefs).toHaveBeenCalledWith("default", false);
  expect(getTimeSeriesData).toHaveBeenCalledWith(
    "default",
    13,
    "2026/111/00:00:00",
    "2026/112/00:00:00",
  );
  expect(result.errors).toEqual([]);
  expect(result.results?.tsIds?.[0]).toEqual(
    expect.objectContaining({
      key: 13,
      uniqueString: "LOC.Flow.Inst.1Hour.0.raw",
    }),
  );
  expect(result.series).toHaveLength(1);
});
