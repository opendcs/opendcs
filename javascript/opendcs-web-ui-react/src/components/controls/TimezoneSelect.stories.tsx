import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { expect, fn, waitFor } from "storybook/test";
import { TimezoneSelect } from "./TimezoneSelect";
import { getTimeZones } from "./timezones";

// Controlled wrapper, mirroring how Site and TransportMedium keep the value in
// their reducers.
type DemoProps = React.ComponentProps<typeof TimezoneSelect>;

const TimezoneSelectDemo: React.FC<DemoProps> = ({
  value: initValue,
  onChange,
  ...rest
}) => {
  const [value, setValue] = useState<string | undefined>(initValue);
  return (
    <TimezoneSelect
      {...rest}
      value={value}
      onChange={(v) => {
        setValue(v);
        onChange(v);
      }}
    />
  );
};

const meta = {
  component: TimezoneSelectDemo,
  args: {
    ariaLabel: "Time Zone",
    edit: true,
    onChange: fn(),
  },
} satisfies Meta<typeof TimezoneSelectDemo>;

export default meta;

type Story = StoryObj<typeof meta>;

// Free text suggests the same list the closed select offers, UTC and GMT included.
export const FreeTextSuggestsSharedList: Story = {
  args: { freeText: true },
  play: async ({ canvas, canvasElement, mount }) => {
    await mount();
    const input = (await canvas.findByLabelText("Time Zone")) as HTMLInputElement;
    const listId = input.getAttribute("list");
    expect(listId).toBeTruthy();
    const datalist = canvasElement.ownerDocument.getElementById(listId!);
    const suggested = Array.from(datalist?.querySelectorAll("option") ?? []).map(
      (o) => o.value,
    );
    expect(suggested).toEqual(getTimeZones());
    expect(suggested.filter((v) => v === "UTC")).toHaveLength(1);
    expect(suggested.filter((v) => v === "GMT")).toHaveLength(1);
  },
};

// A zone outside the list (a logger kept on standard time) can still be typed.
export const FreeTextAcceptsUnlistedZone: Story = {
  args: { freeText: true },
  play: async ({ args, canvas, mount, userEvent }) => {
    await mount();
    const input = (await canvas.findByLabelText("Time Zone")) as HTMLInputElement;
    await userEvent.type(input, "GMT-05:00");
    expect(input.value).toEqual("GMT-05:00");
    await waitFor(() => {
      expect(args.onChange).toHaveBeenLastCalledWith("GMT-05:00");
    });
  },
};

// Read-only shows the saved value with no suggestions attached.
export const FreeTextReadOnly: Story = {
  args: { freeText: true, edit: false, value: "EST5EDT" },
  play: async ({ canvas, canvasElement, mount }) => {
    await mount();
    const input = (await canvas.findByLabelText("Time Zone")) as HTMLInputElement;
    expect(input.value).toEqual("EST5EDT");
    expect(input.readOnly).toBe(true);
    expect(input.hasAttribute("list")).toBe(false);
    expect(canvasElement.querySelector("datalist")).toBeNull();
  },
};
