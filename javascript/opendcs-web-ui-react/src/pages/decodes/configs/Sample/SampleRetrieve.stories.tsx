import type { Meta, StoryObj } from "@storybook/react-vite";

import SampleRetrieve from "./SampleRetrieve";
import { Row } from "react-bootstrap";

const meta = {
  component: SampleRetrieve,
  render: (_Story, context) => {
    return (
      <Row>
        <SampleRetrieve output={context.args.output} />
      </Row>
    );
  },
} satisfies Meta<typeof SampleRetrieve>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    output: {},
  },
};
