import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, fn, Mock, MockedFunction } from "storybook/test";
import { SiteNameList, SiteNameType } from "./SiteNameList";
import { WithRefLists } from "../../../.storybook/mock/WithRefLists";
import { act } from "@testing-library/react";
import { useEffect, useState } from "react";

const meta = {
  component: SiteNameList,
  decorators: [WithRefLists],
} satisfies Meta<typeof SiteNameList>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    siteNames: [],
    actions: {
      add: () => {},
      edit: () => {},
      remove: () => {},
      save: () => {},
    },
  },
};

export const WithNames: Story = {
  args: {
    siteNames: [
      { type: "CWMS", name: "Alder Springs" },
      { type: "NWSHB5", name: "ALS" },
    ],
  },
};

export const WithNamesInEdit: Story = {
  args: {
    siteNames: [
      { type: "CWMS", name: "Alder Springs" },
      { type: "NWSHB5", name: "ALS" },
    ],
    actions: {
      add: fn(),
      save: fn((item) => console.log(`Saving ${JSON.stringify(item)}`)),
      remove: fn(),
    },
    edit: true,
  },
  render: (args) => {
    const { siteNames, actions, edit } = args;
    const [names, updateNames] = useState<SiteNameType[]>([]);

    useEffect(() => {
      updateNames(siteNames as SiteNameType[]);
    }, []);

    const realSave = (item: SiteNameType) => {
      actions?.save?.(item);
      updateNames((prev) => {
        const filtered = prev.filter((sn) => sn.type != item.type);
        return [...filtered, item];
      });
    };

    const realRemove = (item: SiteNameType) => {
      actions?.remove?.(item);
      updateNames((prev) => {
        const filtered = prev.filter((sn) => sn.type != item.type);
        return [...filtered];
      });
    };

    return (
      <SiteNameList
        siteNames={names}
        actions={{ save: realSave, remove: realRemove }}
        edit={edit}
      />
    );
  },
  play: async ({ mount, userEvent, parameters, args }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const saveFn = args.actions?.save! as Mock;

    // const editButton = await canvas.findByRole("button", {
    //   name: i18n.t("sites:site_names.edit_for", {
    //     type: "CWMS",
    //     name: "Alder Springs",
    //   }),
    // });
    // await act(async () => userEvent.click(editButton));
    // const cwmsValue = await canvas.findByRole("textbox", {
    //   name: i18n.t("sites:site_names.value_input_for", { type: "CWMS" }),
    // });
    // expect(cwmsValue).toBeInTheDocument();
    // await act(async () => userEvent.type(cwmsValue, " 2"));
    // const saveButton = await canvas.findByRole("button", {name: i18n.t("sites:site_names.save_for", {type: "CWMS"})});
    // await act(async () => userEvent.click(saveButton));

    // expect(saveFn).toHaveBeenCalled();
    // const saveArgs = saveFn.mock.calls[0];
    // console.log(saveArgs)
    // expect((saveArgs.at(0) as SiteNameType).name).toEqual("Alder Springs 2");
    // expect(canvas.queryByText("Alder Springs 2")).toBeInTheDocument();
  },
};
