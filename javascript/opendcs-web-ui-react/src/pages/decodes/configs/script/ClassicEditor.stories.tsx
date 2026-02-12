import type { Meta, StoryObj } from "@storybook/react-vite";

import ClassicEditor from "./ClassicEditor";

const meta = {
  component: ClassicEditor,
} satisfies Meta<typeof ClassicEditor>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    formatStatements: [],
  },
};

export const WithScript: Story = {
  args: {
    formatStatements: [
      { sequenceNum: 1, label: "stage", format: "/,4f(s,a,6D' ', 1" },
      { sequenceNum: 2, label: "precip", format: "/,4f(s,a,6D' ', 2" },
      { sequenceNum: 3, label: "battery", format: "/,1f(s,a,6D' ', 3" },
    ],
  },
};
