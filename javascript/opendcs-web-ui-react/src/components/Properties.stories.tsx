import type { Meta, StoryObj } from '@storybook/react-vite';

import Properties, { type Property } from './Properties';

const meta = {
  component: Properties,
} satisfies Meta<typeof Properties>;

export default meta;

type Story = StoryObj<typeof meta>;

const theProps: Property[] = [];

export const Empty: Story = {
  args: {
    theProps: theProps,
    addProp: () => {theProps.push({
      name: 'test',
      value: 'test value'
    })},
    classes: ""
  }
};


export const NotEmpty: Story = {
  args: {
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
    addProp: () => {},
    classes: ""
  }
};