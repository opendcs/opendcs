import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import { expect, fn, screen, waitFor } from "storybook/test";
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
