import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, fn, waitFor } from "storybook/test";
import { SinceUntilEditor, type SinceUntilLabels } from "./SinceUntilEditor";

const SINCE_LABELS: SinceUntilLabels = {
  method: "Since",
  now: "Now",
  nowMinus: "Now minus",
  amount: "Time before now",
  calendar: "Calendar",
};
const UNTIL_LABELS: SinceUntilLabels = { ...SINCE_LABELS, method: "Until" };

const meta = {
  component: SinceUntilEditor,
  args: { onChange: fn(), edit: true, labels: SINCE_LABELS },
} satisfies Meta<typeof SinceUntilEditor>;

export default meta;

type Story = StoryObj<typeof meta>;

// "now - 1 day" parses into the Now-minus method with the amount prefilled.
export const SinceNowMinusParsed: Story = {
  args: { kind: "since", value: "now - 1 day" },
  play: async ({ mount }) => {
    const canvas = await mount();
    const method = (await canvas.findByRole("combobox", {
      name: "Since",
    })) as HTMLSelectElement;
    expect(method.value).toEqual("nowMinus");
    const amount = canvas.getByLabelText("Time before now") as HTMLInputElement;
    expect(amount.value).toEqual("1 day");
  },
};

// The buggy "now - null" falls back to the default amount rather than showing "null".
export const SinceNowMinusNullFallsBack: Story = {
  args: { kind: "since", value: "now - null" },
  play: async ({ mount }) => {
    const canvas = await mount();
    const amount = (await canvas.findByLabelText(
      "Time before now",
    )) as HTMLInputElement;
    expect(amount.value).toEqual("1 hour");
  },
};

// "now" parses into the bare Now method (until only).
export const UntilNowParsed: Story = {
  args: { kind: "until", value: "now", labels: UNTIL_LABELS },
  play: async ({ mount }) => {
    const canvas = await mount();
    const method = (await canvas.findByRole("combobox", {
      name: "Until",
    })) as HTMLSelectElement;
    expect(method.value).toEqual("now");
  },
};

// An absolute OpenDCS string parses into the Calendar method with the
// datetime-local input prefilled (day-of-year 032 == Feb 1).
export const CalendarParsed: Story = {
  args: { kind: "since", value: "2024/032 06:15:00" },
  play: async ({ mount }) => {
    const canvas = await mount();
    const method = (await canvas.findByRole("combobox", {
      name: "Since",
    })) as HTMLSelectElement;
    expect(method.value).toEqual("calendar");
    const cal = canvas.getByLabelText("Calendar") as HTMLInputElement;
    expect(cal.value).toEqual("2024-02-01T06:15");
  },
};

// Editing the now-minus amount emits the assembled "now - <amount>" string.
export const ChangeAmountEmits: Story = {
  args: { kind: "since", value: "now - 1 hour" },
  play: async ({ mount, args, userEvent }) => {
    const canvas = await mount();
    const amount = await canvas.findByLabelText("Time before now");
    await userEvent.clear(amount);
    await userEvent.type(amount, "2 days");
    await waitFor(() => expect(args.onChange).toHaveBeenLastCalledWith("now - 2 days"));
  },
};

// Switching the Until method to "Now" emits "now".
export const SwitchUntilToNowEmits: Story = {
  args: { kind: "until", value: "now - 1 hour", labels: UNTIL_LABELS },
  play: async ({ mount, args, userEvent }) => {
    const canvas = await mount();
    const method = await canvas.findByRole("combobox", { name: "Until" });
    await userEvent.selectOptions(method, "now");
    await waitFor(() => expect(args.onChange).toHaveBeenLastCalledWith("now"));
  },
};

// Read-only mode disables the controls.
export const ReadOnly: Story = {
  args: { kind: "since", value: "now - 1 hour", edit: false },
  play: async ({ mount }) => {
    const canvas = await mount();
    const method = (await canvas.findByRole("combobox", {
      name: "Since",
    })) as HTMLSelectElement;
    expect(method).toBeDisabled();
  },
};
