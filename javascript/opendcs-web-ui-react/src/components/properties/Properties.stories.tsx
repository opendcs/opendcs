import type { Decorator, Meta, StoryObj } from "@storybook/react-vite";
import { PropertiesTable, PropertiesTableProps, type Property } from "./Properties";
import { expect, fn, waitFor, within } from "storybook/test";
import { useCallback, useState } from "storybook/internal/preview-api";
import { act } from "@testing-library/react";
import { useReducer } from "react";
import { PropertiesReducer } from "./PropertiesReducer";

const WithActions: Decorator<PropertiesTableProps> = (Story, context) => {
  const [theProps, dispatch] = useReducer(PropertiesReducer, context.args.theProps);

  const saveProp = useCallback(
    (data: Property) => {
      act(() => dispatch({ type: "save_prop", payload: data }));
    },
    [dispatch],
  );

  const removeProp = useCallback(
    (prop: string) => {
      act(() => dispatch({ type: "delete_prop", payload: { name: prop } }));
    },
    [dispatch],
  );

  return (
    <Story
      args={{
        ...context.args,
        theProps: theProps,
        edit: true,
        actions: {
          save: saveProp,
          remove: removeProp,
        },
      }}
    />
  );
};

const meta = {
  component: PropertiesTable,
} satisfies Meta<typeof PropertiesTable>;

export default meta;

type Story = StoryObj<typeof meta>;

const emptyPropList: Property[] = [];

export const StartEmpty: Story = {
  args: {
    theProps: emptyPropList,
    actions: {},
  },
  decorators: [WithActions],
};

export const EmptyAddThenRemove: Story = {
  args: {
    ...StartEmpty.args,
  },
  decorators: [WithActions],
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const add = await canvas.findByRole("button", {
      name: i18n.t("properties:add_prop"),
    });
    await userEvent.click(add);
    const nameInput = await canvas.findByRole("textbox", {
      name: i18n.t("properties:name_input", { name: "1" }),
    });
    expect(nameInput).toBeInTheDocument();
    const remove = await canvas.findByRole("button", {
      name: i18n.t("properties:delete_prop", { name: "1" }),
    });

    await userEvent.click(remove);
  },
};

export const PropertyNameCannotBeBlank: Story = {
  args: {
    ...StartEmpty.args,
  },
  decorators: [WithActions],
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const add = await canvas.findByRole("button", {
      name: i18n.t("properties:add_prop"),
    });
    await userEvent.click(add);

    const nameInput = await canvas.findByRole("textbox", {
      name: i18n.t("properties:name_input", { name: "1" }),
    });
    expect(nameInput).toBeInTheDocument();

    const valueInput = await canvas.findByRole("textbox", {
      name: i18n.t("properties:value_input", { name: "1" }),
    });
    expect(valueInput).toBeInTheDocument();

    // only set the value
    await userEvent.type(valueInput, "testvalue");

    const save = await canvas.findByRole("button", {
      name: i18n.t("properties:save_prop", { name: "1" }),
    });

    await userEvent.click(save);

    await waitFor(async () => {
      const nameInputAfterSave = canvas.queryByRole("textbox", {
        name: i18n.t("properties:name_input", { name: "1" }),
      });
      expect(nameInputAfterSave).toBeInTheDocument();

      expect(nameInputAfterSave).toHaveClass("border-warning");
    });

    const remove = await canvas.findByRole("button", {
      name: i18n.t("properties:delete_prop", { name: "1" }),
    });

    await userEvent.click(remove);
  },
};

export const EmptyAddThenSaveThenRemove: Story = {
  args: {
    ...StartEmpty.args,
  },
  decorators: [WithActions],
  play: async ({ canvasElement, step, mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    await step("add new prop", async () => {
      const add = await canvas.findByRole("button", {
        name: i18n.t("properties:add_prop"),
      });
      await userEvent.click(add);

      const nameInput = await canvas.findByRole("textbox", {
        name: i18n.t("properties:name_input", { name: "1" }),
      });
      expect(nameInput).toBeInTheDocument();
      const valueInput = await canvas.findByRole("textbox", {
        name: i18n.t("properties:value_input", { name: "1" }),
      });
      expect(valueInput).toBeInTheDocument();

      await userEvent.type(nameInput, "testprop");
      await userEvent.type(valueInput, "testvalue");
    });

    const save = await canvas.findByRole("button", {
      name: i18n.t("properties:save_prop", { name: "1" }),
    });
    await step("save prop", async () => {
      await userEvent.click(save);
    });

    const prop = await canvas.findByText("testprop", undefined, { timeout: 3000 });
    expect(prop).not.toBeNull();

    await step("delete prop", async () => {
      const remove = await canvas.findByRole("button", {
        name: i18n.t("properties:delete_prop", { name: "testprop" }),
      });
      await userEvent.click(remove);

      await waitFor(async () => {
        expect(canvas.queryByText("testprop")).toBeNull();
      });
    });
  },
};

const propList: Property[] = [
  { name: "prop1", value: "val1" },
  { name: "prop2", value: "val2" },
  { name: "prop3", value: "val3" },
  { name: "prop4", value: "val4" },
  { name: "prop5", value: "val5" },
  { name: "prop6", value: "val6" },
  { name: "prop7", value: "val7" },
  { name: "prop8", value: "val8" },
];
export const NotEmpty: Story = {
  args: {
    ...StartEmpty.args,
    theProps: propList,
  },
  decorators: [WithActions],
};

export const ReadOnly: Story = {
  args: {
    theProps: propList,
    actions: {},
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const add = canvas.queryByRole("button", { name: i18n.t("properties:add_prop") });
    expect(add).toBeNull();

    const deleteButton = canvas.queryByRole("button", {
      name: i18n.t("properties:delete_prop", { name: "prop1" }),
    });
    expect(deleteButton).toBeNull();

    const edit = canvas.queryByRole("button", {
      name: i18n.t("properties:edit_prop", { name: "prop1" }),
    });

    expect(edit).toBeNull();
  },
};

export const CanEditOnly: Story = {
  args: {
    theProps: [...propList],
    edit: true,
    canAdd: false,
    actions: {
      save: fn(),
    },
  },
  play: async ({ mount, parameters, userEvent, args }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const add = canvas.queryByRole("button", { name: i18n.t("properties:add_prop") });
    expect(add).toBeNull();

    const deleteButton = canvas.queryByRole("button", {
      name: i18n.t("properties:delete_prop", { name: "prop1" }),
    });
    expect(deleteButton).toBeNull();

    const edit = await canvas.findByRole("button", {
      name: i18n.t("properties:edit_prop", { name: "prop1" }),
    });
    await userEvent.click(edit);

    const save = await canvas.findByRole("button", {
      name: i18n.t("properties:save_prop", { name: "prop1" }),
    });
    await userEvent.click(save);
    expect(args.actions.save).toHaveBeenCalled();
  },
};
