import type { Meta, StoryObj } from '@storybook/react-vite';
import { PropertiesTable, type Property } from './Properties';
import { userEvent } from '@storybook/testing-library';
//import { UserEvent } from 'vitest/browser';
import { expect, waitFor, within } from 'storybook/test';
import { useArgs } from 'storybook/internal/preview-api';
import { act } from '@testing-library/react';

userEvent.setup();

const meta = {
  component: PropertiesTable,
  
} satisfies Meta<typeof PropertiesTable>;

export default meta;

type Story = StoryObj<typeof meta>;

export const StartEmpty: Story = {

  args: {
    theProps: [],
    saveProp: () => {console.log("default save")},
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    removeProp: (_prop: string) => {console.log("default remove")},
    editProp: () => {},
    addProp: () => {},
  },
  render: (args) => {
    const [{theProps}, updateArgs] = useArgs();
    function saveProp(data: Property) {
      const tmp = theProps.filter((p: Property) => p.name !== data.name);
      updateArgs({theProps: [...tmp, {name: data.name, value: data.value}]});
      
    }

    function removeProp(prop: string) {
      const tmp = theProps.filter((e: Property) => e.name !== prop);
      updateArgs({theProps: tmp});
    }

    function addProp() {
      const existingNew = theProps
                              .filter((p: Property) => p.state === 'new')
                              .sort((a: Property,b: Property) => {return parseInt(a.name) - parseInt(b.name)});
      
      const idx = existingNew.length > 0 ? parseInt(existingNew[0].name)+1 : 1;
      
      const tmp = [...theProps, {name: `${idx}`, value: '', state: 'new'}]
      updateArgs({theProps: tmp});
    }

    function editProp(prop: string) {
      const tmp = theProps.map ((p: Property) => {
        if (p.name === prop) {
          return {
            ...p,
            state: 'edit'
          };
        } else {
          return p;
        }
      });
      updateArgs({theProps: tmp});
    }

    return (<PropertiesTable {...args} theProps={theProps} saveProp={saveProp} removeProp={removeProp} addProp={addProp} editProp={editProp} />);
  },
};

export const EmptyAddThenRemove: Story = {
  args: {
    ...StartEmpty.args,
  },
  render: StartEmpty.render,
  play: async ({canvas, userEvent}) => {
    // NOTE: needs some actual assertions, but at the moment it's just an empty uneditable row.
    const add = canvas.getByRole('button', {name: '+'});
    await act(async () =>userEvent.click(add));

    const nameInput = await canvas.findByRole("textbox", {name: "name input for property named 1"})
    expect(nameInput).toBeInTheDocument();
    const remove = await canvas.findByRole('button', {name: 'delete property named 1'});
    await act(async () => userEvent.click(remove));
  },
};

export const EmptyAddThenSaveThenRemove: Story = {
  args: {
    ...StartEmpty.args,
  },
  render: StartEmpty.render,
  play: async ({canvasElement, userEvent, step}) => {
    const canvas = within(canvasElement);
    await step("add new prop", async () => {
      const add = canvas.getByRole('button', {name: '+'});
      await act( async () => userEvent.click(add));

      const nameInput = await canvas.findByRole("textbox", {name: "name input for property named 1"})
      expect(nameInput).toBeInTheDocument();
      const valueInput = await canvas.findByRole("textbox", {name: "value input for property named 1"})
      expect(valueInput).toBeInTheDocument();

      await userEvent.type(nameInput, "testprop");
      await userEvent.type(valueInput, "testvalue");
    });

    const save = await canvas.findByRole('button', {name: 'save property named 1'});
    await step("save prop", async () => {
      await userEvent.click(save);
    })

    const prop = await canvas.findByText("testprop", undefined, {timeout:3000});
    expect(prop).not.toBeNull();

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
    ...StartEmpty.args,
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
  render: StartEmpty.render
};
