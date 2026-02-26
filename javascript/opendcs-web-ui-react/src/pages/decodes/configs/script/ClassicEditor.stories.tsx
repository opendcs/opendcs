import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";

import ClassicEditor, {
  ClassicFormatStatememntEditorProperties,
} from "./ClassicEditor";
import { ArgsStoryFn } from "storybook/internal/types";
import { ApiScriptFormatStatement } from "opendcs-api";
//import "storybook/test";
import { useArgs } from "storybook/internal/preview-api";
import React, { useCallback, useEffect, useState } from "react";

const meta = {
  component: ClassicEditor,
} satisfies Meta<typeof ClassicEditor>;

export default meta;

type Story = StoryObj<typeof meta>;

const Wrapper: React.FC<ClassicFormatStatememntEditorProperties> = ({
  formatStatements,
  edit,
  onFormatStatementChange,
}) => {
  const [storyStatements, setStoryStatements] = useState<ApiScriptFormatStatement[]>(
    [],
  );

  // keep the state local, if we call updateArgs then the component complete
  // rerenders and you can only type a single character at a time.
  useEffect(() => {
    setStoryStatements(formatStatements);
  }, []);

  const updateStatements = useCallback(
    (statements: ApiScriptFormatStatement[]) => {
      onFormatStatementChange?.(statements);
      console.log(`Received ${JSON.stringify(statements)}`);
      setStoryStatements(statements);
      console.log("updated");
    },
    [setStoryStatements],
  );
  console.log(`Sending ${JSON.stringify(storyStatements)}`);
  return (
    <ClassicEditor
      formatStatements={storyStatements}
      edit={edit}
      onFormatStatementChange={updateStatements}
    />
  );
};

const StoryRender: ArgsStoryFn<
  ReactRenderer,
  ClassicFormatStatememntEditorProperties
> = (args) => {
  const [{ formatStatements }, _updateArgs] = useArgs();
  return (
    <Wrapper
      formatStatements={formatStatements}
      edit={args.edit}
      onFormatStatementChange={args.onFormatStatementChange}
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
