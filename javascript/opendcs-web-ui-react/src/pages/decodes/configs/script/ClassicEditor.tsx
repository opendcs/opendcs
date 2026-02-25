import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ApiScriptFormatStatement } from "opendcs-api";
import { useTranslation } from "react-i18next";
import DataTable, { type DataTableRef } from "datatables.net-react";
import DT from "datatables.net-bs5";
import "datatables.net-rowreorder";
import { ArrowDownUp } from "react-bootstrap-icons";
import { renderToString } from "react-dom/server";
import hljs from "highlight.js";
import decodes from "../../../../util/languages/decodes";
import "highlight.js/styles/default.css";
import { dtLangs } from "../../../../lang";
import { Form } from "react-bootstrap";
import { useContextWrapper } from "../../../../util/ContextWrapper";

hljs.registerLanguage("decodes", decodes);

// this isn't a hook, it just has "use" as the name.
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export interface ClassicFormatStatememntEditorProperties {
  formatStatements: ApiScriptFormatStatement[];
  onFormatStatementChange?: (statements: ApiScriptFormatStatement[]) => void;
  edit?: boolean;
}

const statementSorter = (a: ApiScriptFormatStatement, b: ApiScriptFormatStatement) =>
  a.sequenceNum! - b.sequenceNum!;

/**
 * Class editor that has each label/statement on a separate row
 * @returns
 */
const ClassicFormatStatementEditor: React.FC<
  ClassicFormatStatememntEditorProperties
> = ({ formatStatements, edit = false, onFormatStatementChange }) => {
  const { toDom } = useContextWrapper();
  const { t, i18n } = useTranslation(["decodes"]);
  const table = useRef<DataTableRef>(null);

  const renderFormat = useCallback(
    (
      data: string,
      type: string,
      row: ApiScriptFormatStatement,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _meta: any,
    ) => {
      if (type === "display") {
        return toDom(
          <Form.Control
            type="input"
            onChange={(e) => {
              const statements = formatStatements.filter(
                (s) => s.sequenceNum !== row.sequenceNum,
              );
              const toChange = formatStatements.find(
                (s) => s.sequenceNum === row.sequenceNum,
              )!;
              const changed = {
                ...toChange,
                format: e.currentTarget.value,
              };
              console.log(`Started with ${JSON.stringify(formatStatements)}`);
              console.log(`filtered to ${JSON.stringify(statements)}`);
              console.log(`now using ${JSON.stringify(changed)}`);
              onFormatStatementChange?.(
                [...statements, changed].toSorted(statementSorter),
              );
            }}
            className="language-decodes m-0 p-0"
            role="textbox"
            contentEditable={edit}
            aria-label={t("decodes:script_editor.format_statements.format_input", {
              label: row.label,
              sequence: row.sequenceNum,
            })}
            defaultValue={data}
          />,
        );
      } else {
        return data;
      }
    },
    [table.current, edit, formatStatements, onFormatStatementChange],
  );

  const renderLabel = useCallback(
    (
      data: string,
      type: string,
      row: ApiScriptFormatStatement,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _meta: any,
    ) => {
      if (type === "display" && edit) {
        return toDom(
          <Form.Control
            type="input"
            onChange={(e) => {
              const statements = formatStatements.filter(
                (s) => s.sequenceNum !== row.sequenceNum,
              );
              const toChange = formatStatements.find(
                (s) => s.sequenceNum === row.sequenceNum,
              )!;
              const changed = {
                ...toChange,
                label: e.currentTarget.value,
              };
              console.log(`now using ${JSON.stringify(changed)}`);
              onFormatStatementChange?.(
                [...statements, changed].toSorted(statementSorter),
              );
            }}
            className="m-0 p-0"
            defaultValue={data}
            name={`input_${row.sequenceNum}`}
            aria-label={t("decodes:script_editor.format_statements.label_input", {
              sequence: row.sequenceNum,
            })}
          />,
        );
      } else {
        return data;
      }
    },
    [table.current, edit, formatStatements, onFormatStatementChange],
  );

  const columns = useMemo(
    () => [
      {
        data: null,
        render: (data: unknown, type: string, _row: unknown, _meta: unknown) => {
          if (type === "display") {
            return renderToString(<ArrowDownUp />);
          } else {
            return data;
          }
        },
      },
      { data: "label", render: renderLabel },
      { data: "format", render: renderFormat },
      { data: null },
    ],
    [renderLabel, renderFormat],
  );

  useEffect(() => {
    // to just handle data interaction better this is disabled for now.
    // need to determine reasonable method of overlapping text input + code output
    //hljs.highlightAll();
  }, [formatStatements, table.current]);

  const onOrderChange = useCallback(
    (statements: ApiScriptFormatStatement[]) => {
      onFormatStatementChange?.(statements.toSorted(statementSorter));
    },
    [onFormatStatementChange],
  );

  useEffect(() => {
    table.current
      ?.dt()
      ?.off("row-reordered")
      .on("row-reordered", function (_event, params) {
        if (params.length === 0) {
          return;
        }

        onOrderChange(formatStatements);
      });
  }, [table.current, formatStatements, onOrderChange]);
  console.log(`data is ${JSON.stringify(formatStatements)}`);
  console.log(table.current);
  return (
    <DataTable
      options={{
        rowReorder: {
          enable: true,
          dataSrc: "sequenceNum",
        },
        language: dtLangs.get(i18n.language),
        search: false,
        scrollY: "7em",
        scrollCollapse: true,
        searching: false,
        paging: false,
        ordering: false,
        info: false,
        responsive: true,
        order: { name: "sequenceNum", dir: "asc" },
      }}
      columns={columns}
      ref={table}
      data={formatStatements}
      className="table table-hover table-striped table-sm tablerow-cursor w-100 border"
    >
      <thead>
        <tr>
          <th></th>
          <th>{t("decodes:script_editor:format_statements.format_label")}</th>
          <th>{t("decodes:script_editor:format_statements.format_statement")}</th>
          <th>{t("translation:actions")}</th>
        </tr>
      </thead>
    </DataTable>
  );
};

export default ClassicFormatStatementEditor;
