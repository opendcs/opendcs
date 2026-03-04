import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef } from "react";
import { ApiScriptFormatStatement } from "opendcs-api";
import { useTranslation } from "react-i18next";
import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import "datatables.net-rowreorder";
import { ArrowDownUp, CaretDown, CaretUp, Trash } from "react-bootstrap-icons";
import { renderToString } from "react-dom/server";
import hljs from "highlight.js";
import decodes from "../../../../util/languages/decodes";
import "highlight.js/styles/default.css";
import { dtLangs } from "../../../../lang";
import { Button, Form } from "react-bootstrap";
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
  const statementRef = useRef<ApiScriptFormatStatement[]>(formatStatements);
  const inputRef = useRef<{ name: string; position: number } | null>(null);

  useEffect(() => {
    console.log(`Statements now ${JSON.stringify(formatStatements)}`);
    statementRef.current = formatStatements;
  }, [formatStatements]);
  // track last edited text box in some way

  const renderFormat = useCallback(
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
            onBlur={() => {
              inputRef.current = null;
            }}
            onFocus={() => {
              inputRef.current = {
                name: `input_format_${row.sequenceNum}`,
                position: 0,
              };
            }}
            onChange={(e) => {
              const statements = statementRef.current.filter(
                (s) => s.sequenceNum !== row.sequenceNum,
              );
              const toChange = statementRef.current.find(
                (s) => s.sequenceNum === row.sequenceNum,
              )!;
              const changed = {
                ...toChange,
                format: e.currentTarget.value,
              };
              inputRef.current = {
                name: `input_format_${row.sequenceNum}`,
                position: 0,
              };
              onFormatStatementChange?.(
                [...statements, changed].toSorted(statementSorter),
              );
            }}
            className="language-decodes m-0 p-0"
            id={`input_format_${row.sequenceNum}`}
            name={`input_format_${row.sequenceNum}`}
            role="textbox"
            contentEditable={edit}
            aria-label={t("decodes:script_editor.format_statements.format_input", {
              label: row.label,
              sequence: row.sequenceNum,
            })}
            value={data}
          />,
        );
      } else {
        return data;
      }
    },
    [],
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
            onBlur={() => {
              inputRef.current = null;
            }}
            onFocus={() => {
              inputRef.current = {
                name: `input_label_${row.sequenceNum}`,
                position: 0,
              };
            }}
            onChange={(e) => {
              const statements = statementRef.current.map((s) => {
                if (s.sequenceNum === row.sequenceNum) {
                  return {
                    ...s,
                    label: e.currentTarget.value,
                  };
                } else {
                  return s;
                }
              });
              inputRef.current = {
                name: `input_label_${row.sequenceNum}`,
                position: 0,
              };
              console.log(`now using ${JSON.stringify(statements)}`);
              onFormatStatementChange?.(statements);
            }}
            className="m-0 p-0"
            value={data}
            id={`input_label_${row.sequenceNum}`}
            name={`input_label_${row.sequenceNum}`}
            aria-label={t("decodes:script_editor.format_statements.label_input", {
              sequence: row.sequenceNum,
            })}
          />,
        );
      } else {
        return data;
      }
    },
    [],
  );

  const renderDragArrows = useCallback(
    (data: unknown, type: string, row: ApiScriptFormatStatement, _meta: unknown) => {
      if (type === "display") {
        return renderToString(
          <ArrowDownUp
            size={"1.5em"}
            aria-label={t("decodes:script_editor.format_statements.drag_control", {
              sequence: row.sequenceNum,
              label: row.label,
            })}
          />,
        );
      } else {
        return data;
      }
    },
    [],
  );

  const columns = useMemo(() => {
    return [
      {
        data: null,
        width: "2em",
        render: renderDragArrows,
      },
      { data: "label", width: "10em", render: renderLabel, defaultContent: "" },
      { data: "format", render: renderFormat, defaultContent: "" },
      { data: null, width: "15em", name: "actions" },
    ];
  }, []);

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
  }, [formatStatements, onOrderChange]);
  const options: DataTableProps["options"] = useMemo(() => {
    return {
      rowReorder: {
        enable: true,
        dataSrc: "sequenceNum",
      },
      language: dtLangs.get(i18n.language),
      search: false,
      scrollY: "15em",
      scrollCollapse: true,
      searching: false,
      paging: false,
      ordering: false,
      info: false,
      responsive: true,
      order: { name: "sequenceNum", dir: "asc" },
      async drawCallback(_settings) {
        const api = this.api();
        console.log(api.table().node().id);
        if (inputRef.current) {
          // don't even ask, this all clearly needs to happen a different way
          await new Promise((resolve) => setTimeout(resolve, 30));
          const input = api
            .table()
            .node()
            .querySelector(
              `input[name="${inputRef.current.name}"]`,
            ) as HTMLInputElement;
          if (input) {
            input.focus();
          } else {
            console.log("element not found");
          }
        }
      },
    };
  }, [i18n.language]);

  useLayoutEffect(() => {
    const dt = table.current?.dt();
    console.log("updating data");
    if (dt) {
      dt.clear();
      dt.rows.add(formatStatements);
      dt.rows().invalidate();
      dt.draw();
    }
  }, [formatStatements]);

  useEffect(() => {
    // for some reason we need this for the initial load
    // it is likely datatables.net is just not the correct choice
    // for these elements.
    const dt = table.current?.dt();
    if (dt) {
      dt.clear();
      dt.rows.add(formatStatements);
      dt.draw();
    }
  }, []);

  const renderActions = useCallback(
    (statement: ApiScriptFormatStatement, _row: any) => {
      if (edit) {
        return toDom(
          <>
            <Button
              onClick={(e) => {
                e.preventDefault();
                const existing = [...statementRef.current];
                const currentSeq = statement.sequenceNum!;
                const newArray: ApiScriptFormatStatement[] = [
                  ...existing.slice(0, currentSeq),
                  { sequenceNum: currentSeq + 1 },
                  ...existing.slice(currentSeq).map((fs) => {
                    return { ...fs, sequenceNum: fs.sequenceNum! + 1 };
                  }),
                ];
                onFormatStatementChange?.(newArray);
              }}
              aria-label={t("decodes:script_editor.format_statements.add_below", {
                sequence: statement.sequenceNum,
                label: statement.label,
              })}
            >
              <CaretDown size={"1.5em"} />
            </Button>
            <Button
              onClick={(e) => {
                e.preventDefault();
                const existing = [...statementRef.current];
                const currentSeq = statement.sequenceNum!;
                const newArray: ApiScriptFormatStatement[] = [
                  ...existing.slice(0, currentSeq - 1),
                  { sequenceNum: currentSeq },
                  ...existing.slice(currentSeq - 1).map((fs) => {
                    return { ...fs, sequenceNum: fs.sequenceNum! + 1 };
                  }),
                ];
                onFormatStatementChange?.(newArray);
              }}
              aria-label={t("decodes:script_editor.format_statements.add_above", {
                sequence: statement.sequenceNum,
                label: statement.label,
              })}
            >
              <CaretUp size={"1.5em"} />
            </Button>
            <Button
              onClick={(e) => {
                e.preventDefault();
                const existing = [...statementRef.current];
                const currentSeq = statement.sequenceNum!;
                const newArray: ApiScriptFormatStatement[] = [
                  ...existing.slice(0, currentSeq - 1),
                  ...existing.slice(currentSeq).map((fs) => {
                    return { ...fs, sequenceNum: fs.sequenceNum! - 1 };
                  }),
                ];
                onFormatStatementChange?.(newArray);
              }}
              aria-label={t("decodes:script_editor.format_statements.delete", {
                sequence: statement.sequenceNum,
                label: statement.label,
              })}
              variant="danger"
            >
              <Trash size={"1.5em"} />
            </Button>
          </>,
        );
      } else {
        return "";
      }
    },
    [edit, formatStatements],
  );

  const slots = {
    actions: renderActions,
  };

  return (
    <DataTable
      options={options}
      columns={columns}
      slots={slots}
      ref={table}
      className="table table-hover table-striped table-sm tablerow-cursor w-100 border"
    >
      <thead>
        <tr>
          <th aria-label={t("translation:drag_order")}></th>
          <th>{t("decodes:script_editor:format_statements.format_label")}</th>
          <th>{t("decodes:script_editor:format_statements.format_statement")}</th>
          <th>{t("translation:actions")}</th>
        </tr>
      </thead>
    </DataTable>
  );
};

export default ClassicFormatStatementEditor;
