import React, { useCallback, useEffect, useLayoutEffect, useRef } from "react";
import type { ApiScriptFormatStatement } from "opendcs-api";
import { Card, Form } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import DataTable, { type DataTableRef } from "datatables.net-react";
import DT from "datatables.net-bs5";
import "datatables.net-rowreorder";
import { ArrowDownUp } from "react-bootstrap-icons";
import { renderToString } from "react-dom/server";
import hljs from "highlight.js";
import decodes from "../../../../util/languages/decodes";
import "highlight.js/styles/default.css";

hljs.registerLanguage("decodes", decodes);

// this isn't a hook, it just has "use" as the name.
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export interface ClassicFormatStatememntEditorProperties {
  formatStatements: ApiScriptFormatStatement[];
  onChange?: (statements: ApiScriptFormatStatement[]) => void;
  edit?: boolean;
}

/**
 * Class editor that has each label/statement on a separate row
 * @returns
 */
const ClassicFormatStatementEditor: React.FC<
  ClassicFormatStatememntEditorProperties
> = ({ formatStatements, onChange, edit = false }) => {
  const { t } = useTranslation(["decodes"]);
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
        return renderToString(
          <pre className="m-1 p-0">
            <code
              className="language-decodes m-0 p-0"
              role="textbox"
              contentEditable={edit}
              aria-label={t("decodes:config.script.format_statement_input", {
                label: row.label,
                sequence: row.sequenceNum,
              })}
            >
              {data}
            </code>
          </pre>,
        );
      } else {
        return data;
      }
    },
    [table],
  );

  const columns = [
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
    { data: "label" },
    { data: "format", render: renderFormat },
    { data: null },
  ];

  useEffect(() => {
    hljs.highlightAll();
  }, [formatStatements, table]);

  useEffect(() => {
    table.current
      ?.dt()
      ?.off("row-reordered")
      .on("row-reordered", function (_event, params) {
        if (params.length === 0) {
          return;
        }
        if (onChange) {
          onChange(formatStatements);
        }
      });
  }, [table.current, formatStatements]);

  return (
    <Card>
      <Card.Header>{t("decodes:script_editor.format_statement_title")}</Card.Header>
      <Card.Body>
        <DataTable
          options={{
            rowReorder: {
              enable: true,
              dataSrc: "sequenceNum",
            },
            search: false,
            scrollY: "20em",
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
              <th>Label</th>
              <th>Format Statement</th>
              <th>Actions</th>
            </tr>
          </thead>
        </DataTable>
      </Card.Body>
    </Card>
  );
};

export default ClassicFormatStatementEditor;
