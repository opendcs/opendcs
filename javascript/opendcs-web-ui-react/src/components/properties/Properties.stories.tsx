import type { Meta, StoryObj } from "@storybook/react-vite";
import { PropertiesTable, type Property } from "./Properties";
import { expect, waitFor, within } from "storybook/test";
import { useArgs, useState } from "storybook/internal/preview-api";
import { act } from "@testing-library/react";

const meta = {
  component: PropertiesTable,
  decorators: [
    (Story, context) => {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const [_, updateArgs] = useArgs();
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
            updateArgs({ theProps: newProps });
            return newProps;
          }),
        );
      }

      function removeProp(prop: string) {
        act(() =>
          setTheProps((prev) => {
            const tmp = prev.filter((e: Property) => e.name !== prop);
            updateArgs({ theProps: tmp });
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
            updateArgs({ theProps: tmp });
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
            updateArgs({ theProps: tmp });
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
  play: async ({ canvasElement, userEvent, mount }) => {
    console.log("***************************mounting");
    await mount();
    const canvas = within(canvasElement);
    const add = canvas.getByRole("button", { name: /add new property/i });
    console.log("***************************Clicking add");
    await act(async () => userEvent.click(add));
    await mount();
    const nameInput = await canvas.findByRole("textbox", {
      name: "name input for property named 1",
    });
    expect(nameInput).toBeInTheDocument();
    const remove = await canvas.findByRole("button", {
      name: "delete property named 1",
    });

    await act(async () => userEvent.click(remove));
  },
};

export const EmptyAddThenSaveThenRemove: Story = {
  args: {
    ...StartEmpty.args,
  },
  play: async ({ canvasElement, userEvent, step, mount }) => {
    await mount();
    const canvas = within(canvasElement);
    await step("add new prop", async () => {
      const add = canvas.getByRole("button", { name: /add new property/i });
      await act(async () => userEvent.click(add));
      await mount();
      const nameInput = await canvas.findByRole("textbox", {
        name: "name input for property named 1",
      });
      expect(nameInput).toBeInTheDocument();
      const valueInput = await canvas.findByRole("textbox", {
        name: "value input for property named 1",
      });
      expect(valueInput).toBeInTheDocument();

      await act(async () => userEvent.type(nameInput, "testprop"));
      await act(async () => userEvent.type(valueInput, "testvalue"));
    });

    const save = await canvas.findByRole("button", { name: "save property named 1" });
    await step("save prop", async () => {
      await act(async () => userEvent.click(save));
    });

    await mount();
    const prop = await canvas.findByText("testprop", undefined, { timeout: 3000 });
    expect(prop).not.toBeNull();

    await step("delete prop", async () => {
      const remove = await canvas.findByRole("button", {
        name: "delete property named testprop",
      });
      await act(async () => userEvent.click(remove));
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
