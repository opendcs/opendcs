import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from "storybook/test";
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

export const Default: Story = {
  play: ({canvas}) => {
    const text = canvas.queryByText("OpenDCS");
    expect(text).toBeInTheDocument();
  }
};