import type { Meta, StoryObj } from '@storybook/react-vite';

import Properties from './Properties';

const meta = {
  component: Properties,
} satisfies Meta<typeof Properties>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {}
};