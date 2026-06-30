import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { expect, fn, screen, waitFor } from "storybook/test";
import { ChooserTable, type ChooserColumnDef, type ChooserMode } from "./ChooserTable";

interface Row {
  id: number;
  name: string;
  category: string;
}

const ROWS: Row[] = [
  { id: 1, name: "Alpha", category: "fruit" },
  { id: 2, name: "Bravo", category: "veg" },
  { id: 3, name: "Charlie", category: "fruit" },
];

const COLUMNS: ChooserColumnDef<Row>[] = [
  { data: "id", header: "Id", type: "num" },
  { data: "name", header: "Name" },
  { data: "category", header: "Category" },
];

interface HarnessProps {
  mode: ChooserMode;
  data?: Row[];
  onSelectionChange: (ids: number[]) => void;
  onRowDoubleClick?: (row: Row) => void;
}

const ChooserHarness: React.FC<HarnessProps> = ({
  mode,
  data = ROWS,
  onSelectionChange,
  onRowDoubleClick,
}) => {
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  return (
    <ChooserTable<Row, number>
      data={data}
      getId={(r) => r.id}
      columns={COLUMNS}
      mode={mode}
      selectedIds={selectedIds}
      onSelectionChange={(ids) => {
        setSelectedIds(ids);
        onSelectionChange(ids);
      }}
      onRowDoubleClick={onRowDoubleClick}
      selectAllAriaLabel="Select all rows"
      rowSelectAriaLabel={(r) => `Select ${r.name}`}
    />
  );
};

const meta = {
  component: ChooserHarness,
  args: {
    onSelectionChange: fn(),
  },
} satisfies Meta<typeof ChooserHarness>;

export default meta;

type Story = StoryObj<typeof meta>;

const readNameOrder = (): string[] =>
  screen
    .getAllByRole("row")
    .slice(1)
    .map((row) => row.querySelectorAll("td")[1]?.textContent ?? "");

export const SingleSelectRowClick: Story = {
  args: { mode: "single" },
  play: async ({ args, mount, userEvent }) => {
    await mount();
    await screen.findByText("Alpha");

    await userEvent.click(screen.getByText("Bravo"));
    await waitFor(() => expect(args.onSelectionChange).toHaveBeenLastCalledWith([2]));

    // Clicking the same row again deselects (single mode toggle).
    await userEvent.click(screen.getByText("Bravo"));
    await waitFor(() => expect(args.onSelectionChange).toHaveBeenLastCalledWith([]));

    // Picking a different row replaces the selection.
    await userEvent.click(screen.getByText("Charlie"));
    await waitFor(() => expect(args.onSelectionChange).toHaveBeenLastCalledWith([3]));
  },
};

export const MultiSelectAndSelectAll: Story = {
  args: { mode: "multi" },
  play: async ({ args, mount, userEvent }) => {
    await mount();
    await screen.findByText("Alpha");

    // Row clicks add to the selection in multi mode.
    await userEvent.click(screen.getByText("Alpha"));
    await waitFor(() => expect(args.onSelectionChange).toHaveBeenLastCalledWith([1]));

    await userEvent.click(screen.getByText("Charlie"));
    await waitFor(() =>
      expect(args.onSelectionChange).toHaveBeenLastCalledWith([1, 3]),
    );

    // Clicking the same row again removes it.
    await userEvent.click(screen.getByText("Alpha"));
    await waitFor(() => expect(args.onSelectionChange).toHaveBeenLastCalledWith([3]));

    // Select-all in the header checkbox.
    const selectAll = screen.getByLabelText("Select all rows") as HTMLInputElement;
    await userEvent.click(selectAll);
    await waitFor(() =>
      expect(args.onSelectionChange).toHaveBeenLastCalledWith([1, 2, 3]),
    );
  },
};

export const CheckboxHeaderIsNotSortable: Story = {
  args: { mode: "multi" },
  play: async ({ mount }) => {
    await mount();
    await screen.findByText("Alpha");

    // The select-all <th> carries data-dt-order="disable" so DataTables'
    // sort-click delegate skips it entirely.
    const selectAll = screen.getByLabelText("Select all rows");
    const th = selectAll.closest("th");
    expect(th?.dataset.dtOrder).toBe("disable");
  },
};

export const SortToggleStaysTwoStep: Story = {
  args: { mode: "single" },
  play: async ({ mount, userEvent }) => {
    await mount();
    await screen.findByText("Alpha");
    expect(readNameOrder()).toEqual(["Alpha", "Bravo", "Charlie"]);

    const nameHeader = screen.getByRole("columnheader", { name: /name/i });

    // 1st click on a non-default-sort column: asc (already in asc → no change).
    await userEvent.click(nameHeader);
    await waitFor(() => expect(nameHeader.getAttribute("aria-sort")).toBe("ascending"));

    // 2nd click: desc.
    await userEvent.click(nameHeader);
    await waitFor(() => expect(readNameOrder()).toEqual(["Charlie", "Bravo", "Alpha"]));
    expect(nameHeader.getAttribute("aria-sort")).toBe("descending");

    // 3rd click: asc again (not "none" — confirms orderSequence: asc,desc).
    await userEvent.click(nameHeader);
    await waitFor(() => expect(readNameOrder()).toEqual(["Alpha", "Bravo", "Charlie"]));
    expect(nameHeader.getAttribute("aria-sort")).toBe("ascending");
  },
};

export const DoubleClickFiresHandler: Story = {
  args: {
    mode: "single",
    onRowDoubleClick: fn(),
  },
  play: async ({ args, mount, userEvent }) => {
    await mount();
    await screen.findByText("Alpha");
    await userEvent.dblClick(screen.getByText("Bravo"));
    await waitFor(() =>
      expect(args.onRowDoubleClick).toHaveBeenCalledWith(
        expect.objectContaining({ id: 2, name: "Bravo" }),
      ),
    );
  },
};

export const EmptyDataShowsZeroRows: Story = {
  args: { mode: "single", data: [] },
  play: async ({ mount }) => {
    await mount();
    // header row + 1 "no data" row (DataTables auto-inserts an empty-state row).
    await waitFor(() => expect(screen.getAllByRole("row")).toHaveLength(2));
  },
};
