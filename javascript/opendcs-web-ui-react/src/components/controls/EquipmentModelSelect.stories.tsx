import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay, http, HttpResponse } from "msw";
import type { ApiEquipmentModelRef } from "opendcs-api";
import { expect, fn, waitFor, within } from "storybook/test";

import { EquipmentModelSelect } from "./EquipmentModelSelect";

// Database order, so the name sort has something to do.
const MODELS: ApiEquipmentModelRef[] = [
  { equipmentId: 2, name: "sutron-9210B", equipmentType: "dcp" },
  { equipmentId: 1, name: "Campbell-CR1000", equipmentType: "dcp" },
  { equipmentId: 3, name: "goes-tx", equipmentType: "transmitter" },
];

const meta = {
  component: EquipmentModelSelect,
  args: { onChange: fn() },
  parameters: {
    msw: {
      handlers: {
        equipmentRefs: http.get("/odcsapi/equipmentrefs", () =>
          HttpResponse.json(MODELS),
        ),
      },
    },
  },
  // The global i18n preload doesn't include this namespace.
  loaders: [
    async ({ parameters }) => {
      await parameters.i18n.loadNamespaces("equipment");
    },
  ],
} satisfies Meta<typeof EquipmentModelSelect>;

export default meta;

type Story = StoryObj<typeof meta>;

const findLoadedSelect = (canvas: ReturnType<typeof within>) =>
  waitFor(() => {
    const select = canvas.getByRole("combobox") as HTMLSelectElement;
    expect(select).toBeEnabled();
    return select;
  });

/**
 * Models are listed by name behind a leading "None" option, with the
 * assigned one selected.
 */
export const ListsModelsByName: Story = {
  args: { value: 2 },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const select = await findLoadedSelect(canvas);
    const options = within(select)
      .getAllByRole("option")
      .map((o) => o.textContent);
    await expect(options).toEqual([
      parameters.i18n.t("equipment:no_equipment_model"),
      "Campbell-CR1000",
      "goes-tx",
      "sutron-9210B",
    ]);
    await expect(select).toHaveValue("2");
  },
};

export const SelectingAModelReportsItsId: Story = {
  play: async ({ mount, args, userEvent }) => {
    const canvas = await mount();
    const select = await findLoadedSelect(canvas);
    await userEvent.selectOptions(select, "goes-tx");
    await expect(args.onChange).toHaveBeenCalledWith(3);
  },
};

export const SelectingNoneClearsTheModel: Story = {
  args: { value: 2 },
  play: async ({ mount, args, parameters, userEvent }) => {
    const canvas = await mount();
    const select = await findLoadedSelect(canvas);
    await userEvent.selectOptions(
      select,
      parameters.i18n.t("equipment:no_equipment_model"),
    );
    await expect(args.onChange).toHaveBeenCalledWith(undefined);
  },
};

export const Loading: Story = {
  parameters: {
    msw: {
      handlers: {
        equipmentRefs: http.get("/odcsapi/equipmentrefs", async () => {
          await delay("infinite");
        }),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const select = canvas.getByRole("combobox");
    await expect(select).toBeDisabled();
    await expect(select).toHaveTextContent(
      parameters.i18n.t("translation:loading_short"),
    );
  },
};

export const Unavailable: Story = {
  parameters: {
    msw: {
      handlers: {
        equipmentRefs: http.get(
          "/odcsapi/equipmentrefs",
          () => new HttpResponse(null, { status: 500 }),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const select = canvas.getByRole("combobox");
    await expect(select).toBeDisabled();
    await waitFor(() =>
      expect(select).toHaveTextContent(
        parameters.i18n.t("translation:unavailable_short"),
      ),
    );
  },
};
