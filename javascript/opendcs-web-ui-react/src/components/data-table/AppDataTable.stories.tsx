import type { Meta, StoryObj } from "@storybook/react-vite";
import { act, useState } from "react";
import { expect, waitFor } from "storybook/test";
import { AppDataTable, type ColumnDef } from "./AppDataTable";
import type { DataTableProps } from "datatables.net-react";

interface Row {
  id: number;
  name: string;
}

// Deliberately not in alphabetical order, and mixed case so the assertions
// also pin DataTables' case-insensitive string sort. Every ordering under
// test produces a distinct sequence:
//   id asc    -> Zulu, alpha, Mike     (the pre-#1662 behaviour)
//   name asc  -> alpha, Mike, Zulu
//   name desc -> Zulu, Mike, alpha
//   id desc   -> Mike, alpha, Zulu
const ROWS: Row[] = [
  { id: 1, name: "Zulu" },
  { id: 2, name: "alpha" },
  { id: 3, name: "Mike" },
];

const columns = (defaultSort?: "asc" | "desc"): ColumnDef<Row>[] => [
  { data: "id", header: "Id", type: "num" },
  { data: "name", header: "Name", type: "string", defaultSort },
];

interface HarnessProps {
  defaultSort?: "asc" | "desc";
  dataTableOptions?: Partial<DataTableProps["options"]>;
  /**
   * Turns `stateSave` on, under this table id, for the stories that pin what
   * is and isn't remembered between visits. Adds a Revisit button that
   * remounts the table, which reloads it from the saved state the way coming
   * back to the page does.
   */
  savedStateId?: string;
  /** Adds the caption's Add button. */
  withAdd?: boolean;
}

const Harness: React.FC<HarnessProps> = ({
  defaultSort,
  dataTableOptions,
  savedStateId,
  withAdd,
}) => {
  const [visit, setVisit] = useState(0);
  return (
    <div data-visit={visit}>
      {savedStateId && (
        <button type="button" onClick={() => setVisit((v) => v + 1)}>
          Revisit
        </button>
      )}
      <AppDataTable<Row, number>
        key={visit}
        data={ROWS}
        getId={(r) => r.id}
        columns={columns(defaultSort)}
        tableId={savedStateId}
        addNew={
          withAdd
            ? { template: (id) => ({ id, name: "" }), ariaLabel: "Add row" }
            : undefined
        }
        dataTableOptions={{
          // stateSave would let one story's saved order leak into the next via
          // localStorage, and responsive collapsing would move the Name cell
          // out of the row. Neither is what most of these stories are pinning.
          stateSave: Boolean(savedStateId),
          responsive: false,
          ...dataTableOptions,
        }}
      />
    </div>
  );
};

const meta = { component: Harness } satisfies Meta<typeof Harness>;
export default meta;

type Story = StoryObj<typeof meta>;

/** Read the rendered Name column, top row first. */
const readNames = async (
  canvasElement: HTMLElement,
  rowCount = ROWS.length,
): Promise<string[]> => {
  await waitFor(() =>
    expect(canvasElement.querySelectorAll("tbody tr")).toHaveLength(rowCount),
  );
  return Array.from(canvasElement.querySelectorAll("tbody tr")).map(
    (row) => row.querySelectorAll("td")[1]?.textContent?.trim() ?? "",
  );
};

// The bug in #1662: with no `defaultSort` the table falls back to DataTables'
// own [[0, "asc"]], sorting by the id column. Pinned so we notice if that
// fallback ever changes — the sub-tables that lead with a meaningful first
// column still rely on it.
export const NoDefaultSortSortsByFirstColumn: Story = {
  args: {},
  play: async ({ canvasElement }) => {
    await expect(await readNames(canvasElement)).toEqual(["Zulu", "alpha", "Mike"]);
  },
};

export const DefaultSortAscending: Story = {
  args: { defaultSort: "asc" },
  play: async ({ canvasElement }) => {
    await expect(await readNames(canvasElement)).toEqual(["alpha", "Mike", "Zulu"]);
  },
};

export const DefaultSortDescending: Story = {
  args: { defaultSort: "desc" },
  play: async ({ canvasElement }) => {
    await expect(await readNames(canvasElement)).toEqual(["Zulu", "Mike", "alpha"]);
  },
};

// `dataTableOptions` is the documented escape hatch, so it has to outrank a
// column's `defaultSort` rather than be silently merged under it.
export const ExplicitOrderOverridesDefaultSort: Story = {
  args: {
    defaultSort: "asc",
    dataTableOptions: { order: [[0, "desc"]] },
  },
  play: async ({ canvasElement }) => {
    await expect(await readNames(canvasElement)).toEqual(["Mike", "alpha", "Zulu"]);
  },
};

type PlayContext = Parameters<NonNullable<Story["play"]>>[0];

/** Remount the table and wait for the fresh one, as a return visit would. */
const revisit = async ({
  canvasElement,
  userEvent,
}: Pick<PlayContext, "canvasElement" | "userEvent">) => {
  const before = Number(
    canvasElement.querySelector("[data-visit]")?.getAttribute("data-visit"),
  );
  const button = canvasElement.querySelector("button") as HTMLButtonElement;
  await act(async () => userEvent.click(button));
  await waitFor(() =>
    expect(canvasElement.querySelector(`[data-visit="${before + 1}"]`)).not.toBeNull(),
  );
};

export const SavedOrderFromAnotherDefaultIsDropped: Story = {
  args: { defaultSort: "asc", savedStateId: "staleOrderTable" },
  play: async ({ mount, canvasElement }) => {
    localStorage.setItem(
      `DataTables_staleOrderTable_${location.pathname}`,
      JSON.stringify({ time: Date.now(), start: 0, length: 10, order: [[0, "asc"]] }),
    );
    await mount();
    await expect(await readNames(canvasElement)).toEqual(["alpha", "Mike", "Zulu"]);
  },
};

// The other half of the bargain: a sort the user picked is still there when
// they come back.
export const UserPickedSortIsRestored: Story = {
  args: { defaultSort: "asc", savedStateId: "pickedOrderTable" },
  play: async ({ mount, canvasElement, userEvent }) => {
    await mount();
    await expect(await readNames(canvasElement)).toEqual(["alpha", "Mike", "Zulu"]);
    const nameHeader = canvasElement.querySelectorAll("thead th")[1];
    await act(async () => userEvent.click(nameHeader));
    await waitFor(async () =>
      expect(await readNames(canvasElement)).toEqual(["Zulu", "Mike", "alpha"]),
    );
    await revisit({ canvasElement, userEvent });
    await expect(await readNames(canvasElement)).toEqual(["Zulu", "Mike", "alpha"]);
  },
};

// Add sorts by the first column so the new row lands on top. That sort is the
// wrapper's doing, so it must not become how the page loads from then on.
export const AddDoesNotChangeTheSavedSort: Story = {
  args: { defaultSort: "asc", savedStateId: "addOrderTable", withAdd: true },
  play: async ({ mount, canvasElement, userEvent }) => {
    const canvas = await mount();
    await expect(await readNames(canvasElement)).toEqual(["alpha", "Mike", "Zulu"]);
    const addBtn = await canvas.findByRole("button", { name: "Add row" });
    await act(async () => userEvent.click(addBtn));
    await waitFor(async () =>
      expect(await readNames(canvasElement, ROWS.length + 1)).toEqual([
        "",
        "Zulu",
        "alpha",
        "Mike",
      ]),
    );
    await revisit({ canvasElement, userEvent });
    await expect(await readNames(canvasElement)).toEqual(["alpha", "Mike", "Zulu"]);
  },
};
