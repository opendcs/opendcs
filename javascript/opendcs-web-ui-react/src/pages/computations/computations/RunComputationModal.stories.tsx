import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import { expect, fn, screen, userEvent, waitFor } from "storybook/test";
import { RunComputationModal } from "./RunComputationModal";

/** A minimal but realistic SSE transcript from /runcomputation. */
const sseTranscript = [
  "event: computation-status",
  "data: Starting computation TestComp",
  "",
  "event: computation-status",
  "data: Wrote 3 values",
  "",
  "event: Results",
  `data: ${JSON.stringify({
    tsIds: [],
    startTime: "2026-06-01T00:00:00Z",
    endTime: "2026-06-02T00:00:00Z",
  })}`,
  "",
].join("\n");

const handlers = {
  runComputation: http.get(
    "/odcsapi/runcomputation",
    () =>
      new HttpResponse(sseTranscript, {
        headers: { "Content-Type": "text/event-stream" },
      }),
  ),
};

const meta = {
  component: RunComputationModal,
  args: {
    show: true,
    computationId: 42,
    computationName: "TestComp",
    onHide: fn(),
  },
  parameters: {
    msw: { handlers },
  },
} satisfies Meta<typeof RunComputationModal>;

export default meta;

type Story = StoryObj<typeof meta>;

/**
 * Guards issue #2060: every control in the dialog must show its translated
 * label, never the raw i18next key it falls back to when a lookup misses.
 */
export const TimeRangeLabels: Story = {
  play: async ({ mount, parameters }) => {
    await mount();
    const { i18n } = parameters;

    expect(
      await screen.findByText(i18n.t("computations:run.title")),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(i18n.t("computations:run.start"))).toBeInTheDocument();
    expect(screen.getByLabelText(i18n.t("computations:run.end"))).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: i18n.t("computations:run.run") }),
    ).toBeInTheDocument();
    // The computation being run is still identified, just not in the title.
    expect(screen.getByText("TestComp")).toBeInTheDocument();
    // No control fell back to its key ("run.title", "run.start", ...).
    expect(screen.queryByText(/^run\./)).toBeNull();
  },
};

/** The trace can be moved into its own window so the dialog stays readable. */
export const TracePopsOut: Story = {
  play: async ({ mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await userEvent.click(
      await screen.findByRole("button", { name: i18n.t("computations:run.run") }),
    );

    // Trace starts docked inside the run dialog.
    expect(await screen.findByText("Wrote 3 values")).toBeInTheDocument();
    expect(screen.getAllByRole("dialog")).toHaveLength(1);

    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("computations:run.log_popout") }),
    );

    await waitFor(() => expect(screen.getAllByRole("dialog")).toHaveLength(2));
    expect(
      screen.getByText(i18n.t("computations:run.log_detached")),
    ).toBeInTheDocument();
    // The trace itself moved rather than being duplicated.
    expect(screen.getAllByLabelText(i18n.t("computations:run.log_label"))).toHaveLength(
      1,
    );
    expect(screen.getByText("Wrote 3 values")).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("computations:run.log_dock") }),
    );

    await waitFor(() => expect(screen.getAllByRole("dialog")).toHaveLength(1));
    expect(
      screen.queryByText(i18n.t("computations:run.log_detached")),
    ).not.toBeInTheDocument();
  },
};

/**
 * A manual run must not write to the database -- the operator reviews the numbers first -- so
 * the computed values travel inline on the Results event. This asserts they are rendered from
 * that payload alone, with no /tsdata request to fall back on.
 */
export const RendersValuesFromResultsPayload: Story = {
  parameters: {
    msw: {
      handlers: {
        runComputation: http.get(
          "/odcsapi/runcomputation",
          () =>
            new HttpResponse(
              [
                "event: computation-status",
                "data: Computed 2 values for 'TESTSITE.Flow.Inst.1Hour.0.rev'",
                "",
                "event: Results",
                `data: ${JSON.stringify({
                  tsIds: [
                    { uniqueString: "TESTSITE.Flow.Inst.1Hour.0.rev", key: 1234 },
                  ],
                  startTime: "2026-06-01T00:00:00Z",
                  endTime: "2026-06-02T00:00:00Z",
                  data: [
                    {
                      tsid: {
                        uniqueString: "TESTSITE.Flow.Inst.1Hour.0.rev",
                        key: 1234,
                        storageUnits: "cms",
                      },
                      values: [
                        { sampleTime: "2026-06-01T00:00:00Z", value: 11.5 },
                        { sampleTime: "2026-06-01T01:00:00Z", value: 12.25 },
                      ],
                    },
                  ],
                })}`,
                "",
              ].join("\n"),
              { headers: { "Content-Type": "text/event-stream" } },
            ),
        ),
        // Any read-back attempt is a regression: the run wrote nothing, so there is nothing
        // to read. Fail loudly rather than letting a fallback mask it.
        tsData: http.get("/odcsapi/tsdata", () => {
          throw new Error(
            "RunComputationModal must not fetch /tsdata for a manual run",
          );
        }),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    await mount();
    const { i18n } = parameters;

    await userEvent.click(
      await screen.findByRole("button", { name: i18n.t("computations:run.run") }),
    );

    expect(await screen.findByText("11.5", {}, { timeout: 5000 })).toBeInTheDocument();
    expect(screen.getByText("12.25")).toBeInTheDocument();
    // The column is labelled from the identifier carried alongside the values.
    expect(
      screen.getByText(/TESTSITE\.Flow\.Inst\.1Hour\.0\.rev \(cms\)/),
    ).toBeInTheDocument();
  },
};

/**
 * An output the run described but returned no series for has to be named, rather than leaving
 * an unexplained gap in the results table.
 */
export const OutputsWithoutValuesAreReported: Story = {
  parameters: {
    msw: {
      handlers: {
        runComputation: http.get(
          "/odcsapi/runcomputation",
          () =>
            new HttpResponse(
              [
                "event: computation-status",
                "data: Computation produced no output time series.",
                "",
                "event: Results",
                `data: ${JSON.stringify({
                  tsIds: [
                    { uniqueString: "TESTSITE.Flow.Inst.1Hour.0.compproc", key: -1 },
                  ],
                  startTime: "2026-06-01T00:00:00Z",
                  endTime: "2026-06-02T00:00:00Z",
                  data: [],
                })}`,
                "",
              ].join("\n"),
              { headers: { "Content-Type": "text/event-stream" } },
            ),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    await mount();
    const { i18n } = parameters;

    await userEvent.click(
      await screen.findByRole("button", { name: i18n.t("computations:run.run") }),
    );

    const warning = await screen.findByText(
      new RegExp(i18n.t("computations:run.unresolved_outputs")),
      {},
      { timeout: 5000 },
    );
    expect(warning).toHaveTextContent("TESTSITE.Flow.Inst.1Hour.0.compproc");
  },
};

/**
 * Screening algorithms express their result as quality flags, and a reviewer deciding whether to
 * keep a run's output needs to see them next to the value. The flag encoding is database
 * specific, so the server sends the rendered form and the dialog shows it verbatim.
 *
 * Deliberately mixes a flagged and an unflagged sample in one series: an unflagged value must
 * stay clean, or the marker means nothing.
 */
export const RendersQualityFlags: Story = {
  parameters: {
    msw: {
      handlers: {
        runComputation: http.get(
          "/odcsapi/runcomputation",
          () =>
            new HttpResponse(
              [
                "event: computation-status",
                "data: Computation executed with 0 errors",
                "",
                "event: Results",
                `data: ${JSON.stringify({
                  tsIds: [{ uniqueString: "TESTSITE.Stage.Inst.1Hour.0.rev", key: 77 }],
                  startTime: "2026-06-01T00:00:00Z",
                  endTime: "2026-06-02T00:00:00Z",
                  data: [
                    {
                      tsid: {
                        uniqueString: "TESTSITE.Stage.Inst.1Hour.0.rev",
                        key: 77,
                        storageUnits: "ft",
                      },
                      values: [
                        // Screened and rejected high -- what a screening run flags.
                        {
                          sampleTime: "2026-06-01T00:00:00Z",
                          value: 998.5,
                          flags: 1073741952,
                          flagsDisplay: "S(R+)",
                        },
                        // Screened, nothing asserted: no marker.
                        { sampleTime: "2026-06-01T01:00:00Z", value: 12.25, flags: 0 },
                      ],
                    },
                  ],
                })}`,
                "",
              ].join("\n"),
              { headers: { "Content-Type": "text/event-stream" } },
            ),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    await mount();
    const { i18n } = parameters;

    await userEvent.click(
      await screen.findByRole("button", { name: i18n.t("computations:run.run") }),
    );

    const flagged = await screen.findByText("S(R+)", {}, { timeout: 5000 });
    expect(flagged).toBeInTheDocument();
    // The raw flag word stays available for anyone who needs the exact bits.
    expect(flagged).toHaveAttribute("title", "flags: 1073741952");

    // The flagged value and the clean one both render, and only one is marked.
    const flaggedCell = flagged.closest("td");
    expect(flaggedCell).toHaveTextContent("998.5");
    expect(screen.getByText("12.25").closest("td")?.textContent).toBe("12.25");
  },
};
