import type { Meta, StoryObj } from '@storybook/react-vite';
import { PropertiesTable, type PropertiesTableProps, type Property } from './Properties';
import { useArgs } from 'storybook/internal/preview-api';
import { userEvent } from '@storybook/testing-library';
import { expect, waitFor, within } from 'storybook/test';

userEvent.setup();

const meta = {
  component: PropertiesTable,
} satisfies Meta<typeof PropertiesTable>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Empty: Story = {

  args: {
    theProps: [],
    saveProp: () => {},
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    removeProp: (_prop: string) => {},
  },
  render: function Render(args: PropertiesTableProps) {
    const [{theProps}, updateArgs] = useArgs();
    console.log("hello");
    function saveProp(data: Property) {
      console.log("prop saved");
      const tmp = theProps.filter((p: Property) => p.name !== data.name);
      updateArgs({theProps: [...tmp, data]});
    }

    function removeProp(prop: string) {
      const tmp = theProps.filter((e: Property) => e.name !== prop);
      updateArgs({theProps: tmp});
    }

    return <PropertiesTable {...args} theProps={theProps} saveProp={saveProp} removeProp={removeProp} />;
  },
};

export const EmptyAddThenRemove: Story = {
  args: {
    ...Empty.args,
  },
  render: Empty.render,
  play: async ({canvas, userEvent}) => {
    // NOTE: needs some actual assertions, but at the moment it's just an empty uneditable row.
    const add = canvas.getByRole('button', {name: '+'});
    await userEvent.click(add);

    const nameInput = canvas.getByRole("textbox", {name: "name input for property named"})
    expect(nameInput).toBeInTheDocument();
    const remove = canvas.getByRole('button', {name: 'delete property named'});
    await userEvent.click(remove);
  },
};

export const EmptyAddThenSaveThenRemove: Story = {
  args: {
    ...Empty.args,
  },
  render: Empty.render,
  play: async ({canvasElement, userEvent, step}) => {
    const canvas = within(canvasElement);
    await step("add new prop", async () => {
      const add = canvas.getByRole('button', {name: '+'});
      await userEvent.click(add);

      const nameInput = canvas.getByRole("textbox", {name: "name input for property named"})
      expect(nameInput).toBeInTheDocument();
      const valueInput = canvas.getByRole("textbox", {name: "value input for property named"})
      expect(valueInput).toBeInTheDocument();

      await userEvent.type(nameInput, "testprop");
      await userEvent.type(valueInput, "testvalue");
    });

    const save = canvas.getByRole('button', {name: 'save property named'});
    await step("save prop", async () => {
      await userEvent.click(save);
    })

    await waitFor(async () => {
      const prop = await canvas.findByText("testprop");
      expect(prop).not.toBeNull();
    }, {timeout: 3000});

    await step("delete prop", async () => {
      const remove = await canvas.findByRole('button', {name: 'delete property named testprop'});
      await userEvent.click(remove);
  
      await waitFor(async () => {
        expect(canvas.queryByText("testprop")).toBeNull();
      });
    });
  }
}


export const NotEmpty: Story = {
  
  args: {
    ...Empty.args,
    theProps: [
      {name: 'prop1', value: 'val1'},
      {name: 'prop2', value: 'val2'},
      {name: 'prop3', value: 'val3'},
      {name: 'prop4', value: 'val4'},
      {name: 'prop5', value: 'val5'},
      {name: 'prop6', value: 'val6'},
      {name: 'prop7', value: 'val7'},
      {name: 'prop8', value: 'val8'},
      
    ],
  },
  render: Empty.render
};
