import type { Meta, StoryObj } from '@storybook/react-vite';

import { PropertiesTable, type PropertiesTableProps, type Property } from './Properties';
import { useArgs } from 'storybook/internal/preview-api';

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
    addProp: () => {},
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    removeProp: (_prop: string) => {},
  },
  render: function Render(args: PropertiesTableProps) {
    const [{theProps}, updateArgs] = useArgs();

    function addProp() {
      const tmp = [...theProps, {name: "", value: ""}];
        updateArgs({theProps: tmp});
    }

    function removeProp(prop: string) {
      const tmp = theProps.filter((e: Property) => e.name !== prop);
      updateArgs({theProps: tmp});
    }

    return <PropertiesTable {...args} theProps={theProps} addProp={addProp} removeProp={removeProp} />;
  }
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