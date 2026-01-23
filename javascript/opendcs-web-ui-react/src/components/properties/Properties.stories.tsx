import type { Meta, StoryObj } from "@storybook/react-vite";
import { PropertiesTable, type Property } from "./Properties";
import { expect, waitFor, within } from "storybook/test";
import { useState } from "storybook/internal/preview-api";
import { act } from "@testing-library/react";

const meta = {
  component: PropertiesTable,
  decorators: [
    (Story, context) => {
      const [theProps, setTheProps] = useState<Property[]>(context.args.theProps);

      function saveProp(data: Property) {
        act(() =>
          setTheProps((prev) => {
            let newProps: Property[];
            if (typeof data.state === "number") {
              newProps = prev.map((p: Property) =>
                `${data.state}` === p.name ? { name: data.name, value: data.value } : p,
              );
            } else if (data.state === "edit") {
              newProps = prev.map((p: Property) =>
                p.name === data.name
                  ? { ...p, value: data.value, state: undefined }
                  : p,
              );
            } else {
              throw new Error(
                `Attempt to save property with invalid state ${data.state}`,
              );
            }
            return newProps;
          }),
        );
      }

      function removeProp(prop: string) {
        act(() =>
          setTheProps((prev) => {
            const tmp = prev.filter((e: Property) => e.name !== prop);
            return tmp;
          }),
        );
      }

      function addProp() {
        act(() =>
          setTheProps((prev) => {
            const existingNew = prev
              .filter((p: Property) => p.state === "new")
              .sort((a: Property, b: Property) => {
                return parseInt(a.name) - parseInt(b.name);
              });
            const idx = existingNew.length > 0 ? parseInt(existingNew[0].name) + 1 : 1;

            const tmp = [
              ...prev,
              { name: `${idx}`, value: "", state: "new" } as Property,
            ];
            return tmp;
          }),
        );
      }

      function editProp(prop: string) {
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
      }
      return (
        <Story
          args={{
            ...context.args,
            theProps: theProps,
            saveProp: saveProp,
            removeProp: removeProp,
            addProp: addProp,
            editProp: editProp,
          }}
        />
      );
    },
  ],
} satisfies Meta<typeof PropertiesTable>;

export default meta;

type Story = StoryObj<typeof meta>;

const propList: Property[] = [];

export const StartEmpty: Story = {
  args: {
    theProps: propList,
    saveProp: () => {
      console.log("default save");
    },
    removeProp: () => {
      console.log("default remove");
    },
    editProp: () => {
      console.log("default edit");
    },
    addProp: () => {
      console.log("default add");
    },
  },
};

export const EmptyAddThenRemove: Story = {
  args: {
    ...StartEmpty.args,
  },
  play: async ({ canvasElement, mount, parameters, userEvent }) => {
    const { i18n } = parameters;
    await mount();
    const canvas = within(canvasElement);
    const add = await canvas.findByRole("button", { name: i18n.t("properties:add_prop") });
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
    ...StartEmpty.args
  },
  play: async ({ canvasElement, mount, parameters, userEvent }) => {
    const { i18n } = parameters;
    await mount();
    const canvas = within(canvasElement);
    const add = await canvas.findByRole("button", { name: i18n.t("properties:add_prop") });
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
    });


    const remove = await canvas.findByRole("button", {
      name: i18n.t("properties:delete_prop", { name: "1" }),
    });

    await userEvent.click(remove);
    await mount();
  },
}

export const EmptyAddThenSaveThenRemove: Story = {
  args: {
    ...StartEmpty.args,
  },
  play: async ({ canvasElement, step, mount, parameters, userEvent }) => {
    const { i18n } = parameters;
    await mount();
    const canvas = within(canvasElement);
    await step("add new prop", async () => {
      const add = await canvas.findByRole("button", { name: i18n.t("properties:add_prop") });
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

export const NotEmpty: Story = {
  args: {
    ...StartEmpty.args,
    theProps: [
      { name: "prop1", value: "val1" },
      { name: "prop2", value: "val2" },
      { name: "prop3", value: "val3" },
      { name: "prop4", value: "val4" },
      { name: "prop5", value: "val5" },
      { name: "prop6", value: "val6" },
      { name: "prop7", value: "val7" },
      { name: "prop8", value: "val8" },
    ],
  },
};
