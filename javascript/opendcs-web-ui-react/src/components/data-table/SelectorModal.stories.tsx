import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { expect, fn, screen, waitFor } from "storybook/test";
import { SelectorModal } from "./SelectorModal";
import type { ChooserColumnDef } from "./ChooserTable";

interface Row {
  id: number;
  name: string;
}

const ROWS: Row[] = [
  { id: 1, name: "Alpha" },
  { id: 2, name: "Bravo" },
];

const COLUMNS: ChooserColumnDef<Row>[] = [
  { data: "id", header: "Id", type: "num" },
  { data: "name", header: "Name" },
];

interface HarnessProps {
  data?: Row[];
  loading?: boolean;
  onSelect: (row: Row) => void | Promise<void>;
}

const SelectorHarness: React.FC<HarnessProps> = ({
  data = ROWS,
  loading = false,
  onSelect,
}) => {
  const [show, setShow] = useState(true);
  return (
    <SelectorModal<Row, number>
      show={show}
      onHide={() => setShow(false)}
      onSelect={async (row) => {
        await onSelect(row);
        setShow(false);
      }}
      title="Pick a Row"
      noneMessage="No rows found."
      data={data}
      loading={loading}
      getId={(r) => r.id}
      columns={COLUMNS}
    />
  );
};

const meta = {
  component: SelectorHarness,
  args: { onSelect: fn() },
} satisfies Meta<typeof SelectorHarness>;

export default meta;

type Story = StoryObj<typeof meta>;

export const SelectButtonDisabledUntilRowChosen: Story = {
  play: async ({ mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    await screen.findByText("Pick a Row");

    const selectBtn = screen.getByRole("button", {
      name: i18n.t("translation:select"),
    });
    expect(selectBtn).toBeDisabled();

    await userEvent.click(screen.getByText("Alpha"));
    await waitFor(() => expect(selectBtn).toBeEnabled());
  },
};

export const SelectFiresWithChosenRowAndCloses: Story = {
  play: async ({ args, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    await screen.findByText("Pick a Row");

    await userEvent.click(screen.getByText("Bravo"));
    const selectBtn = screen.getByRole("button", {
      name: i18n.t("translation:select"),
    });
    await waitFor(() => expect(selectBtn).toBeEnabled());
    await userEvent.click(selectBtn);

    await waitFor(() =>
      expect(args.onSelect).toHaveBeenCalledWith(
        expect.objectContaining({ id: 2, name: "Bravo" }),
      ),
    );
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument(), {
      timeout: 5000,
    });
  },
};

export const CancelClosesWithoutFiringSelect: Story = {
  play: async ({ args, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    await screen.findByText("Pick a Row");

    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:cancel") }),
    );
    await waitFor(() =>
      expect(screen.queryByText("Pick a Row")).not.toBeInTheDocument(),
    );
    expect(args.onSelect).not.toHaveBeenCalled();
  },
};

export const EmptyDataShowsNoneMessage: Story = {
  args: { data: [] },
  play: async ({ mount }) => {
    await mount();
    expect(await screen.findByText("No rows found.")).toBeInTheDocument();
  },
};

export const LoadingShowsSpinner: Story = {
  args: { data: [], loading: true },
  play: async ({ mount }) => {
    await mount();
    // Bootstrap Spinner — Modal portals to document.body, so query via screen.
    expect(await screen.findByRole("status")).toBeInTheDocument();
  },
};
