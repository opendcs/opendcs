import type { Meta, StoryObj } from '@storybook/react-vite';

import Properties, { type Property } from './Properties';

const meta = {
  component: Properties,
  argTypes: {
    theProps: {
      control: {
        type: 'object'
      }
    }
  }
} satisfies Meta<typeof Properties>;

export default meta;

type Story = StoryObj<typeof meta>;

// eslint-disable-next-line no-var
var theProps: Property[] = [];

export const Empty: Story = {

  args: {
    theProps: theProps,
    addProp: () => {theProps.push({
      name: 'test',
      value: 'test value'
    })},
    removeProp: (p: string) => {
      console.log(p)
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const tmp = theProps.filter((pa: Property, _idx: number, _array: Property[]) => {
        console.log(pa.name)
        console.log(pa.name !== p);
        return pa.name !== p;
    });
      Empty.args.theProps = tmp;
    },
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
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    removeProp: (_p: string) => {},
    classes: ""
  }
};