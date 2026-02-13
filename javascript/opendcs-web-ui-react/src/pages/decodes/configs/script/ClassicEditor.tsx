import React, { useEffect, useRef } from "react";
import type { ApiScriptFormatStatement } from "opendcs-api";
import { Card, ListGroup, Table } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import "datatables.net-rowreorder";
import { ArrowDownUp } from "react-bootstrap-icons";
import { renderToString } from "react-dom/server";

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
    { data: "format" },
    { data: null },
  ];

  useEffect(() => {
    table.current
      ?.dt()
      ?.off("row-reordered")
      .on("row-reordered", function (e, params) {
        if (params.length === 0) {
          return;
        }
        if (onChange) {
          console.log(`new format statements ${JSON.stringify(formatStatements)}`);
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
            searching: false,
            paging: false,
            ordering: false,
            responsive: true,
            order: { name: "sequenceNum", dir: "asc" },
          }}
          columns={columns}
          ref={table}
          data={formatStatements}
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
