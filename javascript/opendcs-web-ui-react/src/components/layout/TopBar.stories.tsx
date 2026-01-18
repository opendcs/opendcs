import type { Meta, StoryObj } from '@storybook/react-vite';

import { TopBar } from './TopBar';
import { ModeIcons } from '../ModeIcon';

const meta = {
  component: TopBar,
  decorators: [
    (Story) => {
      return (
        <>
          <ModeIcons />
          <Story />
        </>
      );
    },
  ]
} satisfies Meta<typeof TopBar>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};