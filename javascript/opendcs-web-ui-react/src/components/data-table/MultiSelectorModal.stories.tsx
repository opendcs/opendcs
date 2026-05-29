import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState, type ReactNode } from "react";
import { expect, fn, screen, waitFor } from "storybook/test";
import { MultiSelectorModal } from "./MultiSelectorModal";
import type { ChooserColumnDef } from "./ChooserTable";

interface Row {
  id: number;
  name: string;
}

const ROWS: Row[] = [
  { id: 1, name: "Alpha" },
  { id: 2, name: "Bravo" },
  { id: 3, name: "Gamma" },
];

const COLUMNS: ChooserColumnDef<Row>[] = [
  { data: "id", header: "Id", type: "num" },
  { data: "name", header: "Name" },
];

interface HarnessProps {
  data?: Row[];
  loading?: boolean;
  excludeIds?: number[];
  confirmPending?: boolean;
  closeOnConfirm?: boolean;
  cancelLabel?: ReactNode;
  banner?: ReactNode;
  onAdd: (ids: number[]) => void;
}

const Harness: React.FC<HarnessProps> = ({
  data = ROWS,
  loading = false,
  excludeIds = [],
  confirmPending = false,
  closeOnConfirm = true,
  cancelLabel,
  banner,
  onAdd,
}) => {
  const [show, setShow] = useState(true);
  return (
    <MultiSelectorModal<Row, number>
      show={show}
      onHide={() => setShow(false)}
      onAdd={onAdd}
      title="Pick Rows"
      noneMessage="No rows available."
      confirmLabel={(count) => `Add ${count}`}
      data={data}
      loading={loading}
      getId={(r) => r.id}
      columns={COLUMNS}
      excludeIds={excludeIds}
      confirmPending={confirmPending}
      closeOnConfirm={closeOnConfirm}
      cancelLabel={cancelLabel}
      banner={banner}
    />
  );
};

const meta = {
  component: Harness,
  args: { onAdd: fn() },
} satisfies Meta<typeof Harness>;

export default meta;

type Story = StoryObj<typeof meta>;

// Confirm is disabled until at least one row is selected.
export const AddDisabledUntilSelection: Story = {
  play: async ({ mount, userEvent }) => {
    await mount();
    await screen.findByText("Pick Rows");
    expect(screen.getByRole("button", { name: "Add 0" })).toBeDisabled();
    await userEvent.click(screen.getByText("Alpha"));
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Add 1" })).toBeEnabled(),
    );
  },
};

// Selecting multiple rows and confirming fires onAdd with the ids in data order
// and closes the modal.
export const SelectMultipleAndAdd: Story = {
  play: async ({ mount, args, userEvent }) => {
    await mount();
    await screen.findByText("Pick Rows");
    await userEvent.click(screen.getByText("Alpha"));
    await userEvent.click(screen.getByText("Gamma"));
    await userEvent.click(screen.getByRole("button", { name: "Add 2" }));
    await waitFor(() => expect(args.onAdd).toHaveBeenCalledWith([1, 3]));
    await waitFor(() =>
      expect(screen.queryByText("Pick Rows")).not.toBeInTheDocument(),
    );
  },
};

// Cancel closes the modal without firing onAdd.
export const CancelClosesWithoutAdding: Story = {
  play: async ({ mount, args, userEvent }) => {
    await mount();
    await screen.findByText("Pick Rows");
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));
    await waitFor(() =>
      expect(screen.queryByText("Pick Rows")).not.toBeInTheDocument(),
    );
    expect(args.onAdd).not.toHaveBeenCalled();
  },
};

// Empty data shows the none-message.
export const EmptyShowsNoneMessage: Story = {
  args: { data: [] },
  play: async ({ mount }) => {
    await mount();
    expect(await screen.findByText("No rows available.")).toBeInTheDocument();
  },
};

// Loading with no data yet shows a spinner.
export const LoadingShowsSpinner: Story = {
  args: { data: [], loading: true },
  play: async ({ mount }) => {
    await mount();
    expect(await screen.findByRole("status")).toBeInTheDocument();
  },
};

// excludeIds filters already-attached rows out of the chooser.
export const ExcludeFiltersRows: Story = {
  args: { excludeIds: [2] },
  play: async ({ mount }) => {
    await mount();
    await screen.findByText("Pick Rows");
    expect(await screen.findByText("Alpha")).toBeInTheDocument();
    expect(screen.getByText("Gamma")).toBeInTheDocument();
    expect(screen.queryByText("Bravo")).not.toBeInTheDocument();
  },
};

// A pending confirm shows a spinner and keeps the button disabled.
export const ConfirmPendingDisables: Story = {
  args: { confirmPending: true },
  play: async ({ mount, userEvent }) => {
    await mount();
    await screen.findByText("Pick Rows");
    await userEvent.click(screen.getByText("Alpha"));
    // Spinner replaces the confirm label while pending.
    expect(await screen.findByRole("status")).toBeInTheDocument();
  },
};

// With closeOnConfirm=false the modal stays open after confirming (the caller
// drives close on success).
export const StaysOpenWhenCloseOnConfirmFalse: Story = {
  args: { closeOnConfirm: false },
  play: async ({ mount, args, userEvent }) => {
    await mount();
    await screen.findByText("Pick Rows");
    await userEvent.click(screen.getByText("Alpha"));
    await userEvent.click(screen.getByRole("button", { name: "Add 1" }));
    await waitFor(() => expect(args.onAdd).toHaveBeenCalledWith([1]));
    expect(screen.getByText("Pick Rows")).toBeInTheDocument();
  },
};

// Banner content and a custom cancel label both render.
export const BannerAndCustomCancel: Story = {
  // "Dismiss" rather than "Close" — react-bootstrap's header close button is
  // already named "Close", so a distinct label keeps the query unambiguous.
  args: { banner: <div>Heads up</div>, cancelLabel: "Dismiss" },
  play: async ({ mount }) => {
    await mount();
    await screen.findByText("Pick Rows");
    expect(screen.getByText("Heads up")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Dismiss" })).toBeInTheDocument();
  },
};
