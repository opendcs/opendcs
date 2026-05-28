import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, fn, waitFor } from "storybook/test";
import { SinceUntilEditor } from "./SinceUntilEditor";

const meta = {
  component: SinceUntilEditor,
  args: { onChange: fn(), edit: true },
} satisfies Meta<typeof SinceUntilEditor>;

export default meta;

type Story = StoryObj<typeof meta>;

// "now - 1 day" parses into the Now-minus method with the amount prefilled.
export const SinceNowMinusParsed: Story = {
  args: { kind: "since", value: "now - 1 day" },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const method = (await canvas.findByRole("combobox", {
      name: i18n.t("routing:since"),
    })) as HTMLSelectElement;
    expect(method.value).toEqual("nowMinus");
    const amount = canvas.getByLabelText(
      i18n.t("routing:now_minus_amount"),
    ) as HTMLInputElement;
    expect(amount.value).toEqual("1 day");
  },
};

// The buggy "now - null" falls back to the default amount rather than showing "null".
export const SinceNowMinusNullFallsBack: Story = {
  args: { kind: "since", value: "now - null" },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const amount = (await canvas.findByLabelText(
      i18n.t("routing:now_minus_amount"),
    )) as HTMLInputElement;
    expect(amount.value).toEqual("1 hour");
  },
};

// "now" parses into the bare Now method (until only).
export const UntilNowParsed: Story = {
  args: { kind: "until", value: "now" },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const method = (await canvas.findByRole("combobox", {
      name: i18n.t("routing:until"),
    })) as HTMLSelectElement;
    expect(method.value).toEqual("now");
  },
};

// An absolute OpenDCS string parses into the Calendar method with the
// datetime-local input prefilled (day-of-year 032 == Feb 1).
export const CalendarParsed: Story = {
  args: { kind: "since", value: "2024/032 06:15:00" },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const method = (await canvas.findByRole("combobox", {
      name: i18n.t("routing:since"),
    })) as HTMLSelectElement;
    expect(method.value).toEqual("calendar");
    const cal = canvas.getByLabelText(i18n.t("routing:calendar")) as HTMLInputElement;
    expect(cal.value).toEqual("2024-02-01T06:15");
  },
};

// Editing the now-minus amount emits the assembled "now - <amount>" string.
export const ChangeAmountEmits: Story = {
  args: { kind: "since", value: "now - 1 hour" },
  play: async ({ mount, args, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const amount = await canvas.findByLabelText(i18n.t("routing:now_minus_amount"));
    await userEvent.clear(amount);
    await userEvent.type(amount, "2 days");
    await waitFor(() => expect(args.onChange).toHaveBeenLastCalledWith("now - 2 days"));
  },
};

// Switching the Until method to "Now" emits "now".
export const SwitchUntilToNowEmits: Story = {
  args: { kind: "until", value: "now - 1 hour" },
  play: async ({ mount, args, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const method = await canvas.findByRole("combobox", {
      name: i18n.t("routing:until"),
    });
    await userEvent.selectOptions(method, "now");
    await waitFor(() => expect(args.onChange).toHaveBeenLastCalledWith("now"));
  },
};

// Read-only mode disables the controls.
export const ReadOnly: Story = {
  args: { kind: "since", value: "now - 1 hour", edit: false },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const method = (await canvas.findByRole("combobox", {
      name: i18n.t("routing:since"),
    })) as HTMLSelectElement;
    expect(method).toBeDisabled();
  },
};
