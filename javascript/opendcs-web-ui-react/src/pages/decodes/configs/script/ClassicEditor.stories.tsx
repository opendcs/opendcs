import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";

import ClassicEditor, {
  ClassicFormatStatememntEditorProperties,
} from "./ClassicEditor";
import { ArgsStoryFn } from "storybook/internal/types";
import { ApiScriptFormatStatement } from "opendcs-api";
//import "storybook/test";
import { useArgs } from "storybook/internal/preview-api";
import React, { useCallback, useEffect, useState } from "react";
import { expect, userEvent } from "storybook/test";
import { act } from "@testing-library/react";

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
    formatStatements: [{ sequenceNum: 1 }],
    edit: true,
  },
  render: StoryRender,
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

export const WithScriptInsertRows: Story = {
  args: {
    formatStatements: [{ sequenceNum: 1, label: "Stage", format: "not used" }],
    edit: true,
  },
  render: StoryRender,
  play: async ({ mount, parameters, canvasElement }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const addBelow = await canvas.findByRole("button", {
      name: i18n.t("decodes:script_editor.format_statements.add_below", {
        sequence: 1,
        label: "Stage",
      }),
    });
    await act(async () => userEvent.click(addBelow));
    await new Promise((resolve) => setTimeout(resolve, 100));
    console.log(canvasElement);
    const newBelowRowDelete = await canvas.findByRole("button", {
      name: i18n.t("decodes:script_editor.format_statements.delete", {
        sequence: 2,
        label: "",
      }),
    });
    await userEvent.click(newBelowRowDelete);
    await new Promise((resolve) => setTimeout(resolve, 100));

    const addAbove = await canvas.findByRole("button", {
      name: i18n.t("decodes:script_editor.format_statements.add_above", {
        sequence: 1,
        label: "Stage",
      }),
    });
    await userEvent.click(addAbove);
    const newAboveRowDelete = await canvas.findByRole("button", {
      name: i18n.t("decodes:script_editor.format_statements.delete", {
        sequence: 1,
        label: "",
      }),
    });
    await userEvent.click(newAboveRowDelete);
    await new Promise((resolve) => setTimeout(resolve, 100));

    const stageLabelInputRow = await canvas.findByRole("textbox", {
      name: i18n.t("decodes:script_editor.format_statements.label_input", {
        sequence: 1,
        label: "Stage",
      }),
    });
    expect(stageLabelInputRow).toBeInTheDocument();
  },
};
