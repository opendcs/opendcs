import type { Meta, StoryObj } from '@storybook/react-vite';
import { PropertiesTable, type PropertiesTableProps, type Property } from './Properties';
import { useArgs } from 'storybook/internal/preview-api';
import { userEvent } from '@storybook/testing-library';
import { expect } from 'storybook/test';

userEvent.setup();

const meta = {
  component: PropertiesTable,
} satisfies Meta<typeof PropertiesTable>;

export default meta;

type Story = StoryObj<typeof meta>;

// eslint-disable-next-line no-var
var theProps: Property[] = [];

export const Empty: Story = {

  args: {
    theProps: theProps,
    saveProp: () => {},
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    removeProp: (_prop: string) => {},
  },
  render: function Render(args: PropertiesTableProps) {
    const [{theProps}, updateArgs] = useArgs();

    function saveProp(data: Property) {
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
    
    const remove = canvas.getByRole('button', {name: 'delete property named '});
    await userEvent.click(remove);
  },
};


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
