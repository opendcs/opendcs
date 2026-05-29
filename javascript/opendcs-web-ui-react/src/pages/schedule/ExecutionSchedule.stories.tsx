import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { expect, fn, waitFor } from "storybook/test";
import { ExecutionSchedule, type ExecutionScheduleChange } from "./ExecutionSchedule";

// Controlled wrapper so the component sees state updates without us having to
// reach into React internals. Mirrors how `Schedule.tsx` drives it.
type WrapperProps = {
  startTime?: Date;
  runInterval?: string;
  timeZone?: string;
  edit?: boolean;
  onChange: (change: ExecutionScheduleChange) => void;
  onTimeZoneChange: (value: string) => void;
};

const ExecutionScheduleDemo: React.FC<WrapperProps> = ({
  startTime: initStart,
  runInterval: initInterval,
  timeZone: initTz,
  edit = true,
  onChange,
  onTimeZoneChange,
}) => {
  const [startTime, setStartTime] = useState<Date | undefined>(initStart);
  const [runInterval, setRunInterval] = useState<string | undefined>(initInterval);
  const [timeZone, setTimeZone] = useState<string | undefined>(initTz);

  return (
    <ExecutionSchedule
      startTime={startTime}
      runInterval={runInterval}
      timeZone={timeZone}
      edit={edit}
      onChange={(change) => {
        setStartTime(change.startTime);
        setRunInterval(change.runInterval);
        onChange(change);
      }}
      onTimeZoneChange={(value) => {
        setTimeZone(value);
        onTimeZoneChange(value);
      }}
    />
  );
};

const meta = {
  component: ExecutionScheduleDemo,
  args: {
    edit: true,
    onChange: fn(),
    onTimeZoneChange: fn(),
  },
} satisfies Meta<typeof ExecutionScheduleDemo>;

export default meta;

type Story = StoryObj<typeof meta>;

// No startTime + no runInterval → "Run Continuously" mode. Start time and
// timezone are disabled because they have no effect, and the periodic amount
// input is disabled too.
export const RunContinuously: Story = {
  play: async ({ canvas, mount, parameters }) => {
    await mount();
    const { i18n } = parameters;
    const cont = (await canvas.findByRole("radio", {
      name: i18n.t("schedule:run_continuously"),
    })) as HTMLInputElement;
    expect(cont.checked).toBe(true);
    const start = canvas.getByLabelText(
      i18n.t("schedule:start_time"),
    ) as HTMLInputElement;
    expect(start.disabled).toBe(true);
    const tz = canvas.getByLabelText(i18n.t("schedule:timezone")) as HTMLSelectElement;
    expect(tz.disabled).toBe(true);
    const amount = canvas.getByLabelText(
      i18n.t("schedule:run_amount"),
    ) as HTMLInputElement;
    expect(amount.disabled).toBe(true);
  },
};

// startTime set, no runInterval → "Run Once" mode. Start time is enabled, the
// periodic amount/unit stay disabled.
export const RunOnce: Story = {
  args: {
    startTime: new Date("2024-06-15T10:30:00"),
    timeZone: "UTC",
  },
  play: async ({ canvas, mount, parameters }) => {
    await mount();
    const { i18n } = parameters;
    const once = (await canvas.findByRole("radio", {
      name: i18n.t("schedule:run_once"),
    })) as HTMLInputElement;
    expect(once.checked).toBe(true);
    const start = canvas.getByLabelText(
      i18n.t("schedule:start_time"),
    ) as HTMLInputElement;
    expect(start.disabled).toBe(false);
    expect(start.value).not.toEqual("");
    const amount = canvas.getByLabelText(
      i18n.t("schedule:run_amount"),
    ) as HTMLInputElement;
    expect(amount.disabled).toBe(true);
  },
};

// startTime + parseable runInterval → "Run Every" mode. Amount and unit are
// populated and editable; start-time + tz are enabled.
export const RunEvery: Story = {
  args: {
    startTime: new Date("2024-06-15T10:30:00"),
    runInterval: "30 minutes",
    timeZone: "America/New_York",
  },
  play: async ({ canvas, mount, parameters }) => {
    await mount();
    const { i18n } = parameters;
    const every = (await canvas.findByRole("radio", {
      name: i18n.t("schedule:run_every"),
    })) as HTMLInputElement;
    expect(every.checked).toBe(true);
    const amount = canvas.getByLabelText(
      i18n.t("schedule:run_amount"),
    ) as HTMLInputElement;
    expect(amount.value).toEqual("30");
    expect(amount.disabled).toBe(false);
    const unit = canvas.getByLabelText(
      i18n.t("schedule:run_unit"),
    ) as HTMLSelectElement;
    expect(unit.value).toEqual("minute");
  },
};

// Switching from Continuously to Once enables the start-time picker and
// populates it with a default value so the user has something to refine.
export const SwitchToOnceEnablesStartTime: Story = {
  play: async ({ canvas, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    const once = await canvas.findByRole("radio", {
      name: i18n.t("schedule:run_once"),
    });
    await userEvent.click(once);
    await waitFor(() => {
      const start = canvas.getByLabelText(
        i18n.t("schedule:start_time"),
      ) as HTMLInputElement;
      expect(start.disabled).toBe(false);
      expect(start.value).not.toEqual("");
    });
  },
};

// Switching to Run Every populates an interval too and onChange fires with
// the encoded `<n> <unit>` string the API expects.
export const SwitchToRunEveryEmitsInterval: Story = {
  play: async ({ args, canvas, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    const every = await canvas.findByRole("radio", {
      name: i18n.t("schedule:run_every"),
    });
    await userEvent.click(every);
    await waitFor(() => {
      expect(args.onChange).toHaveBeenCalledWith(
        expect.objectContaining({
          runInterval: expect.stringMatching(/^\d+\s+(minute|hour|day)$/),
        }),
      );
    });
  },
};

// Typing into the amount input updates the runInterval on every keystroke,
// preserving the unit. We don't care about exact intermediate calls, just
// that the latest emitted interval starts with the new amount.
export const EditAmountUpdatesInterval: Story = {
  args: {
    startTime: new Date("2024-06-15T10:30:00"),
    runInterval: "2 hour",
    timeZone: "UTC",
  },
  play: async ({ args, canvas, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    const amount = (await canvas.findByLabelText(
      i18n.t("schedule:run_amount"),
    )) as HTMLInputElement;
    await userEvent.clear(amount);
    await userEvent.type(amount, "12");
    await waitFor(() => {
      expect(args.onChange).toHaveBeenLastCalledWith(
        expect.objectContaining({ runInterval: "12 hour" }),
      );
    });
  },
};

// Changing the unit select preserves the count.
export const EditUnitUpdatesInterval: Story = {
  args: {
    startTime: new Date("2024-06-15T10:30:00"),
    runInterval: "5 minute",
    timeZone: "UTC",
  },
  play: async ({ args, canvas, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    const unit = (await canvas.findByLabelText(
      i18n.t("schedule:run_unit"),
    )) as HTMLSelectElement;
    await userEvent.selectOptions(unit, "day");
    await waitFor(() => {
      expect(args.onChange).toHaveBeenLastCalledWith(
        expect.objectContaining({ runInterval: "5 day" }),
      );
    });
  },
};

// Selecting Run Continuously nulls both startTime and runInterval atomically
// — the API contract for the "always running" mode.
export const SwitchToContinuouslyClearsBothFields: Story = {
  args: {
    startTime: new Date("2024-06-15T10:30:00"),
    runInterval: "1 hour",
    timeZone: "UTC",
  },
  play: async ({ args, canvas, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    const cont = await canvas.findByRole("radio", {
      name: i18n.t("schedule:run_continuously"),
    });
    await userEvent.click(cont);
    await waitFor(() => {
      expect(args.onChange).toHaveBeenCalledWith({
        startTime: undefined,
        runInterval: undefined,
      });
    });
  },
};

// edit=false freezes the whole control: every radio, the start-time picker,
// the timezone select, and the periodic amount/unit are all disabled.
export const ReadOnly: Story = {
  args: {
    edit: false,
    startTime: new Date("2024-06-15T10:30:00"),
    runInterval: "1 hour",
    timeZone: "UTC",
  },
  play: async ({ canvas, mount, parameters }) => {
    await mount();
    const { i18n } = parameters;
    const cont = (await canvas.findByRole("radio", {
      name: i18n.t("schedule:run_continuously"),
    })) as HTMLInputElement;
    expect(cont.disabled).toBe(true);
    const start = canvas.getByLabelText(
      i18n.t("schedule:start_time"),
    ) as HTMLInputElement;
    expect(start.disabled).toBe(true);
    const tz = canvas.getByLabelText(i18n.t("schedule:timezone")) as HTMLSelectElement;
    expect(tz.disabled).toBe(true);
    const amount = canvas.getByLabelText(
      i18n.t("schedule:run_amount"),
    ) as HTMLInputElement;
    expect(amount.disabled).toBe(true);
  },
};

// Changing the timezone fires onTimeZoneChange independently of the
// startTime/runInterval pair.
export const EditTimezoneFiresCallback: Story = {
  args: {
    startTime: new Date("2024-06-15T10:30:00"),
    runInterval: "1 hour",
    timeZone: "UTC",
  },
  play: async ({ args, canvas, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    const tz = (await canvas.findByLabelText(
      i18n.t("schedule:timezone"),
    )) as HTMLSelectElement;
    // Pick whatever non-UTC option the runtime offers — Intl exposes plenty.
    const nonUtc = Array.from(tz.options).find((o) => o.value && o.value !== "UTC");
    if (!nonUtc) throw new Error("expected at least one non-UTC timezone option");
    await userEvent.selectOptions(tz, nonUtc.value);
    await waitFor(() => {
      expect(args.onTimeZoneChange).toHaveBeenCalledWith(nonUtc.value);
    });
  },
};
