import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";

import ClassicEditor, {
  ClassicFormatStatememntEditorProperties,
} from "./ClassicEditor";
import { ArgsStoryFn } from "storybook/internal/types";
import { ApiScriptFormatStatement } from "../../../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useCallback, useEffect, useState } from "storybook/internal/preview-api";

const meta = {
  component: ClassicEditor,
} satisfies Meta<typeof ClassicEditor>;

export default meta;

type Story = StoryObj<typeof meta>;

const StoryRender: ArgsStoryFn<
  ReactRenderer,
  ClassicFormatStatememntEditorProperties
> = (args) => {
  const [storyStatements, setStoryStatements] = useState<ApiScriptFormatStatement[]>(
    [],
  );

  useEffect(() => {
    setStoryStatements(args.formatStatements);
  }, []);

  const onChange = useCallback(
    (statements: ApiScriptFormatStatement[]) => {
      args.onChange?.(statements);
      setStoryStatements(
        statements.toSorted((a, b) => a.sequenceNum! - b.sequenceNum!),
      );
    },
    [setStoryStatements],
  );

  return (
    <ClassicEditor
      formatStatements={storyStatements}
      onChange={onChange}
      edit={args.edit}
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
};

export const WithScriptEdit: Story = {
  args: {
    formatStatements: simpleScript,
    edit: true,
  },
  render: StoryRender,
};
