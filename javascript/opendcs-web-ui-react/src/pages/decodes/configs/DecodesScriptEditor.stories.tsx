import type { Meta, StoryObj } from "@storybook/react-vite";

import { DecodesScriptEditor } from "./DecodesScriptEditor";
import { ApiConfigScriptDataOrderEnum } from "opendcs-api";

const meta = {
  component: DecodesScriptEditor,
} satisfies Meta<typeof DecodesScriptEditor>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};

export const WithScript: Story = {
  args: {
    script: {
      name: "Test Script 1",
      dataOrder: ApiConfigScriptDataOrderEnum.D,
      headerType: "goes",
    },
  },
};
