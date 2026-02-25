import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";

import ClassicEditor, {
  ClassicFormatStatememntEditorProperties,
} from "./ClassicEditor";
import { ArgsStoryFn } from "storybook/internal/types";
import { ApiScriptFormatStatement } from "opendcs-api";
//import "storybook/test";
import { useArgs, useCallback } from "storybook/internal/preview-api";

const meta = {
  component: ClassicEditor,
} satisfies Meta<typeof ClassicEditor>;

export default meta;

type Story = StoryObj<typeof meta>;

const StoryRender: ArgsStoryFn<
  ReactRenderer,
  ClassicFormatStatememntEditorProperties
> = (args) => {
  const [{ formatStatements }, updateArgs] = useArgs();

  console.log("hello?");
  const updateStatements = useCallback(
    (statements: ApiScriptFormatStatement[]) => {
      args.onFormatStatementChange?.(statements);
      console.log(`Received ${JSON.stringify(statements)}`);
      updateArgs({
        formatStatements: statements,
      });
      console.log("updated");
    },
    [updateArgs],
  );
  console.log("rending classic editor");
  return (
    <ClassicEditor
      formatStatements={formatStatements}
      edit={args.edit}
      onFormatStatementChange={updateStatements}
    />
  );
};

export const Default: Story = {
  args: {
    formatStatements: [],
  },
};

const simpleScript: ApiScriptFormatStatement[] = [
  { sequenceNum: 1, label: "stage", format: "/,4f(s,a,6D' ', 1)" },
  { sequenceNum: 2, label: "precip", format: "/,4f(s,a,6D' ', 2)" },
  { sequenceNum: 3, label: "battery", format: "/,1f(s,a,6D' ', 3)" },
];

export const WithScript: Story = {
  args: {
    formatStatements: simpleScript,
  },
  render: StoryRender,
  play: async ({ mount }) => {
    const _canvas = await mount();
  },
};

export const WithScriptEdit: Story = {
  args: {
    formatStatements: simpleScript,
    edit: true,
  },
  render: StoryRender,
  play: async ({ mount }) => {
    const _canvas = await mount();
  },
};
