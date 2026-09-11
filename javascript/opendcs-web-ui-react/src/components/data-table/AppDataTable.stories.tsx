import type { Meta, StoryObj } from "@storybook/react-vite";
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
}

const Harness: React.FC<HarnessProps> = ({ defaultSort, dataTableOptions }) => (
  <AppDataTable<Row, number>
    data={ROWS}
    getId={(r) => r.id}
    columns={columns(defaultSort)}
    dataTableOptions={{
      // stateSave would let one story's saved order leak into the next via
      // localStorage, and responsive collapsing would move the Name cell out
      // of the row. Neither is what these stories are pinning.
      stateSave: false,
      responsive: false,
      ...dataTableOptions,
    }}
  />
);

const meta = { component: Harness } satisfies Meta<typeof Harness>;
export default meta;

type Story = StoryObj<typeof meta>;

/** Read the rendered Name column, top row first. */
const readNames = async (canvasElement: HTMLElement): Promise<string[]> => {
  await waitFor(() =>
    expect(canvasElement.querySelectorAll("tbody tr")).toHaveLength(ROWS.length),
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
