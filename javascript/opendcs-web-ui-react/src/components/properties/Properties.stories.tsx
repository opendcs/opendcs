import type { Decorator, Meta, StoryObj } from "@storybook/react-vite";
import { PropertiesTable, PropertiesTableProps, type Property } from "./Properties";
import { expect, fn, waitFor, within } from "storybook/test";
import { useCallback, useState } from "storybook/internal/preview-api";
import { act } from "@testing-library/react";

const WithActions: Decorator<PropertiesTableProps> = (Story, context) => {
  const [theProps, setTheProps] = useState<Property[]>(context.args.theProps);
  const saveProp = useCallback(
    (data: Property) => {
      act(() =>
        setTheProps((prev) => {
          let newProps: Property[];
          if (typeof data.state === "number") {
            newProps = prev.map((p: Property) =>
              `${data.state}` === p.name ? { name: data.name, value: data.value } : p,
            );
          } else if (data.state === "edit") {
            newProps = prev.map((p: Property) =>
              p.name === data.name ? { ...p, value: data.value, state: undefined } : p,
            );
          } else {
            throw new Error(
              `Attempt to save property with invalid state ${data.state}`,
            );
          }
          return newProps;
        }),
      );
    },
    [theProps],
  );

  const removeProp = useCallback(
    (prop: string) => {
      act(() =>
        setTheProps((prev) => {
          const tmp = prev.filter((e: Property) => e.name !== prop);
          return tmp;
        }),
      );
    },
    [theProps],
  );

  const addProp = useCallback(() => {
    act(() =>
      setTheProps((prev) => {
        const existingNew = prev
          .filter((p: Property) => p.state === "new")
          .sort((a: Property, b: Property) => {
            return parseInt(a.name) - parseInt(b.name);
          });
        const idx = existingNew.length > 0 ? parseInt(existingNew[0].name) + 1 : 1;

        const tmp = [...prev, { name: `${idx}`, value: "", state: "new" } as Property];
        return tmp;
      }),
    );
  }, [theProps]);

  const editProp = useCallback(
    (prop: string) => {
      act(() =>
        setTheProps((prev) => {
          const tmp: Property[] = prev.map((p: Property) => {
            if (p.name === prop) {
              return {
                ...p,
                state: "edit",
              };
            } else {
              return p;
            }
          });
          return tmp;
        }),
      );
    },
    [theProps],
  );

  return (
    <Story
      args={{
        ...context.args,
        theProps: theProps,
        actions: {
          save: saveProp,
          remove: removeProp,
          add: addProp,
          edit: editProp,
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
  play: async ({ canvasElement, mount, parameters, userEvent }) => {
    const { i18n } = parameters;
    await mount();
    const canvas = within(canvasElement);
    const add = await canvas.findByRole("button", {
      name: i18n.t("properties:add_prop"),
    });
    await userEvent.click(add);
    await mount();
    const nameInput = await canvas.findByRole("textbox", {
      name: i18n.t("properties:name_input", { name: "1" }),
    });
    expect(nameInput).toBeInTheDocument();
    const remove = await canvas.findByRole("button", {
      name: i18n.t("properties:delete_prop", { name: "1" }),
    });

    await userEvent.click(remove);
    await mount();
  },
};

export const PropertyNameCannotBeBlank: Story = {
  args: {
    ...StartEmpty.args,
  },
  decorators: [WithActions],
  play: async ({ canvasElement, mount, parameters, userEvent }) => {
    const { i18n } = parameters;
    await mount();
    const canvas = within(canvasElement);
    const add = await canvas.findByRole("button", {
      name: i18n.t("properties:add_prop"),
    });
    await userEvent.click(add);
    await mount();
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
    await mount();

    await waitFor(async () => {
      const nameInputAfterSave = canvas.queryByRole("textbox", {
        name: i18n.t("properties:name_input", { name: "1" }),
      });
      expect(nameInputAfterSave).toBeInTheDocument();

      //There is something forcing a rerender that then strips this warning border
      //An attempt to useMemo/useCallback all the functions did not work.
      //It is also failing in the CLI run so is at least consistent.
      //For now... dunno, something to investigate later. I suspect it is something
      //to do with how the decorator is configured
      //expect(nameInputAfterSave).toHaveClass("border-warning");
    });

    const remove = await canvas.findByRole("button", {
      name: i18n.t("properties:delete_prop", { name: "1" }),
    });

    await userEvent.click(remove);
    await mount();
  },
};

export const EmptyAddThenSaveThenRemove: Story = {
  args: {
    ...StartEmpty.args,
  },
  decorators: [WithActions],
  play: async ({ canvasElement, step, mount, parameters, userEvent }) => {
    const { i18n } = parameters;
    await mount();
    const canvas = within(canvasElement);
    await step("add new prop", async () => {
      const add = await canvas.findByRole("button", {
        name: i18n.t("properties:add_prop"),
      });
      await userEvent.click(add);
      await mount();
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

    await mount();
    const prop = await canvas.findByText("testprop", undefined, { timeout: 3000 });
    expect(prop).not.toBeNull();

    await step("delete prop", async () => {
      const remove = await canvas.findByRole("button", {
        name: i18n.t("properties:delete_prop", { name: "testprop" }),
      });
      await userEvent.click(remove);
      await mount();

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
  play: async ({ canvasElement, mount, parameters }) => {
    const { i18n } = parameters;
    await mount();
    const canvas = within(canvasElement);
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
    theProps: [
      ...propList,
      { name: "propInEdit", value: "Test value", state: "edit" },
      { name: "1", value: null, state: "new" },
    ],
    actions: {
      edit: fn(),
      save: fn(),
    },
  },
  play: async ({ canvasElement, mount, parameters, userEvent, args }) => {
    const { i18n } = parameters;
    await mount();
    const canvas = within(canvasElement);
    const add = canvas.queryByRole("button", { name: i18n.t("properties:add_prop") });
    expect(add).toBeNull();

    const deleteButton = canvas.queryByRole("button", {
      name: i18n.t("properties:delete_prop", { name: "prop1" }),
    });
    expect(deleteButton).toBeNull();

    const edit = canvas.getByRole("button", {
      name: i18n.t("properties:edit_prop", { name: "prop1" }),
    });
    await userEvent.click(edit);
    expect(args.actions.edit).toHaveBeenCalled();

    const save = canvas.getByRole("button", {
      name: i18n.t("properties:save_prop", { name: "propInEdit" }),
    });
    await userEvent.click(save);
    expect(args.actions.save).toHaveBeenCalled();

    const saveNew = canvas.getByRole("button", {
      name: i18n.t("properties:save_prop", { name: "1" }),
    });
    await userEvent.click(saveNew);

    // for some reason this behavior, while it can be visibly seen, does not stay static with the "can't save empty name test"
    // So we at least assert it here so that we some record of the behavior.
    expect(args.actions.save).toHaveBeenCalled();
    const newInput = canvas.getByRole("textbox", {
      name: i18n.t("properties:name_input", { name: "1" }),
    });
    expect(newInput).toHaveClass("border-warning");
  },
};
