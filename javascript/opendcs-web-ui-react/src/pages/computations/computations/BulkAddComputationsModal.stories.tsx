import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import type { ApiAlgorithmRef } from "opendcs-api";
import { useCallback, useState } from "react";
import { expect, fn, screen, waitFor } from "storybook/test";
import { BulkAddComputationsModal } from "./BulkAddComputationsModal";

const mockAlgorithmRefs: ApiAlgorithmRef[] = [
  {
    algorithmId: 10,
    algorithmName: "AverageAlgorithm",
    execClass: "decodes.comp.AverageAlgorithm",
    description: "Computes average values.",
  },
  {
    algorithmId: 11,
    algorithmName: "MaxToDate",
    execClass: "decodes.comp.MaxToDate",
    description: "Computes max value to date.",
  },
  {
    algorithmId: 12,
    algorithmName: "SumAlgorithm",
    execClass: "decodes.comp.SumAlgorithm",
    description: "Computes sum of values.",
  },
];

const handlers = {
  algorithmRefs: http.get("/odcsapi/algorithmrefs", () =>
    HttpResponse.json<ApiAlgorithmRef[]>(mockAlgorithmRefs),
  ),
};

const emptyAlgorithmRefsHandler = http.get("/odcsapi/algorithmrefs", () =>
  HttpResponse.json<ApiAlgorithmRef[]>([]),
);

type BulkAddDemoProps = {
  onAdd: (algorithms: ApiAlgorithmRef[]) => Promise<void> | void;
};

const BulkAddDemo: React.FC<BulkAddDemoProps> = ({ onAdd }) => {
  const [show, setShow] = useState(true);

  const handleAdd = useCallback(
    async (algos: ApiAlgorithmRef[]) => {
      await onAdd(algos);
      setShow(false);
    },
    [onAdd],
  );

  return (
    <BulkAddComputationsModal
      show={show}
      onHide={() => setShow(false)}
      onAdd={handleAdd}
    />
  );
};

const meta = {
  component: BulkAddDemo,
  args: { onAdd: fn() },
  parameters: {
    msw: { handlers },
    docs: {
      description: {
        component:
          "Modal for bulk-creating computations from existing algorithm definitions. " +
          "Fetches all algorithm refs on open, lets the user filter and multi-select, " +
          "then calls `onAdd` with the chosen refs when confirmed. " +
          "Requires `ApiContext` to be provided (uses the default value when no provider is present).",
      },
    },
  },
  tags: ["autodocs"],
} satisfies Meta<typeof BulkAddDemo>;

export default meta;

type Story = StoryObj<typeof meta>;

/** Happy path: algorithms load and are listed in the table. */
export const WithAlgorithms: Story = {
  parameters: {
    docs: {
      description: {
        story:
          "All three mock algorithm refs are fetched and displayed once the MSW handler resolves.",
      },
    },
  },
  play: async ({ mount, parameters }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText(i18n.t("computations:add_from_algorithms.title"));
    expect(await screen.findByText("AverageAlgorithm")).toBeInTheDocument();
    expect(await screen.findByText("MaxToDate")).toBeInTheDocument();
    expect(await screen.findByText("SumAlgorithm")).toBeInTheDocument();
  },
};

/** API returns an empty list — the modal shows a "none found" message instead of a table. */
export const Empty: Story = {
  parameters: {
    msw: { handlers: { algorithmRefs: emptyAlgorithmRefsHandler } },
    docs: {
      description: {
        story:
          "When the server returns no algorithm refs the modal body shows a localised " +
          '"none found" message rather than an empty table.',
      },
    },
  },
  play: async ({ mount, parameters }) => {
    await mount();
    const { i18n } = parameters;
    expect(
      await screen.findByText(i18n.t("computations:add_from_algorithms.none_found")),
    ).toBeInTheDocument();
  },
};

/**
 * Typing in the search box narrows the list to algorithms whose name, exec-class,
 * or description contains the query string (case-insensitive).
 */
export const FilterAlgorithms: Story = {
  parameters: {
    docs: {
      description: {
        story:
          'Typing "Max" into the filter input hides non-matching rows immediately, ' +
          "without a network round-trip (filtering is done client-side on the loaded refs).",
      },
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText("AverageAlgorithm");

    await userEvent.type(
      screen.getByRole("searchbox", {
        name: i18n.t("computations:editor.select_algorithm_filter"),
      }),
      "Max",
    );

    await waitFor(() => {
      expect(screen.queryByText("MaxToDate")).toBeInTheDocument();
      expect(screen.queryByText("AverageAlgorithm")).not.toBeInTheDocument();
      expect(screen.queryByText("SumAlgorithm")).not.toBeInTheDocument();
    });
  },
};

/** Clicking a row toggles its selection and enables the Add button once at least one row is selected. */
export const SelectRow: Story = {
  parameters: {
    docs: {
      description: {
        story:
          "Clicking any row selects it (highlighted in blue) and changes the Add button " +
          "label to reflect the current selection count. The button is disabled when nothing is selected.",
      },
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await userEvent.click(await screen.findByText("AverageAlgorithm"));

    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: i18n.t("computations:add_from_algorithms.add_selected", { count: 1 }),
        }),
      ).not.toBeDisabled(),
    );
  },
};

/**
 * The header checkbox selects every visible (filtered) row at once.
 * Clicking it again deselects all of them.
 */
export const ToggleAll: Story = {
  parameters: {
    docs: {
      description: {
        story:
          "The header checkbox follows the standard indeterminate-checkbox pattern: " +
          "checked when all visible rows are selected, unchecked otherwise. " +
          "Clicking it toggles all currently visible rows in one action.",
      },
    },
  },
  play: async ({ mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText("AverageAlgorithm");

    const selectAllBox = screen.getByRole("checkbox", {
      name: i18n.t("computations:add_from_algorithms.select_all"),
    });

    await userEvent.click(selectAllBox);
    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: i18n.t("computations:add_from_algorithms.add_selected", { count: 3 }),
        }),
      ).not.toBeDisabled(),
    );

    // Toggle again to deselect all
    await userEvent.click(selectAllBox);
    await waitFor(() =>
      expect(
        screen.getByRole("button", {
          name: i18n.t("computations:add_from_algorithms.add_selected", { count: 0 }),
        }),
      ).toBeDisabled(),
    );
  },
};

/**
 * Selecting multiple rows and confirming calls `onAdd` with exactly those refs,
 * then closes the modal.
 */
export const SelectAndAdd: Story = {
  parameters: {
    docs: {
      description: {
        story:
          "Two rows are selected and the Add button is clicked. " +
          "`onAdd` is called with the two matching `ApiAlgorithmRef` objects " +
          "and the modal unmounts immediately after.",
      },
    },
  },
  play: async ({ args, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await userEvent.click(await screen.findByText("AverageAlgorithm"));
    await userEvent.click(await screen.findByText("MaxToDate"));

    const addBtn = screen.getByRole("button", {
      name: i18n.t("computations:add_from_algorithms.add_selected", { count: 2 }),
    });
    expect(addBtn).not.toBeDisabled();
    await userEvent.click(addBtn);

    await waitFor(() =>
      expect(args.onAdd).toHaveBeenCalledWith(
        expect.arrayContaining([
          expect.objectContaining({ algorithmId: 10 }),
          expect.objectContaining({ algorithmId: 11 }),
        ]),
      ),
    );
    await waitFor(() =>
      expect(
        screen.queryByText(i18n.t("computations:add_from_algorithms.title")),
      ).not.toBeInTheDocument(),
    );
  },
};

/** Clicking Cancel dismisses the modal without calling `onAdd`. */
export const Cancel: Story = {
  parameters: {
    docs: {
      description: {
        story:
          "The Cancel button closes the modal and leaves `onAdd` uncalled, " +
          "discarding any selections the user had made.",
      },
    },
  },
  play: async ({ args, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText(i18n.t("computations:add_from_algorithms.title"));

    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:cancel") }),
    );

    await waitFor(() =>
      expect(
        screen.queryByText(i18n.t("computations:add_from_algorithms.title")),
      ).not.toBeInTheDocument(),
    );
    expect(args.onAdd).not.toHaveBeenCalled();
  },
};
