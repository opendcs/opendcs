import { useMemo } from "react";
import { Card, Col, Placeholder, Row } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiConfigScript, ApiConfigSensor, ApiDecodedMessage } from "opendcs-api";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";
import type { RemoveAction } from "../../../util/Actions";
import { DecodesScriptEditor } from "./DecodesScriptEditor";

const SCRIPT_EDITOR_PLACEHOLDER: React.FC<{ className?: string }> = ({ className }) => (
  <Card className={className}>
    <Card.Body>
      <Placeholder animation="glow" className="d-block mb-3">
        <Placeholder xs={4} className="rounded" style={{ height: "2rem" }} />
      </Placeholder>
      <Row className="g-2">
        <Col xs={12}>
          <Placeholder animation="glow" className="d-block">
            <Placeholder xs={12} className="rounded" style={{ height: "12rem" }} />
          </Placeholder>
        </Col>
        <Col xs={12}>
          <Placeholder animation="glow" className="d-block">
            <Placeholder xs={12} className="rounded" style={{ height: "8rem" }} />
          </Placeholder>
        </Col>
      </Row>
    </Card.Body>
  </Card>
);

const stmtCount = (row: ApiConfigScript): number =>
  row.formatStatements ? row.formatStatements.length : 0;

const sensorCount = (row: ApiConfigScript): number =>
  row.scriptSensors ? row.scriptSensors.length : 0;

export interface ConfigScriptsTableProperties {
  scripts: ApiConfigScript[];
  configSensors: ApiConfigSensor[];
  /** Imperative decode invocation for the embedded sample tester. */
  decodeData: (raw: string) => ApiDecodedMessage;
  actions?: {
    save?: (script: ApiConfigScript, originalName?: string) => void;
  } & RemoveAction<string>;
  edit?: boolean;
}

export const ConfigScriptsTable: React.FC<ConfigScriptsTableProperties> = ({
  scripts,
  configSensors,
  decodeData,
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["configs", "translation"]);

  const columns = useMemo<ColumnDef<ApiConfigScript>[]>(
    () => [
      {
        data: "name",
        header: t("configs:script_name"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "headerType",
        header: t("configs:header_type"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "dataOrder",
        header: t("configs:data_order"),
        defaultContent: "",
        type: "string",
      },
      {
        data: null,
        header: "# Statements",
        defaultContent: "0",
        type: "num",
        render: (_d, _ty, row) => stmtCount(row),
      },
      {
        data: null,
        header: "# Sensors",
        defaultContent: "0",
        type: "num",
        render: (_d, _ty, row) => sensorCount(row),
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<ApiConfigScript>[]>(
    () =>
      edit
        ? [
            {
              key: "edit",
              icon: "bi-pencil",
              variant: "warning",
              aria: (row) => t("configs:edit_script", { name: row.name }),
              onClick: ({ row, api }) => api.setMode(row, "edit"),
            },
            {
              key: "delete",
              icon: "bi-trash",
              variant: "danger",
              show: (row) => Boolean(row.name),
              aria: (row) => t("configs:delete_script_for", { name: row.name }),
              onClick: ({ row }) => {
                if (row.name) actions.remove?.(row.name);
              },
            },
          ]
        : [
            {
              key: "view",
              icon: "bi-eye",
              variant: "info",
              aria: (row) => t("configs:edit_script", { name: row.name }),
              onClick: ({ row, api }) => api.setMode(row, "show"),
            },
          ],
    [edit, t, actions],
  );

  return (
    <AppDataTable<ApiConfigScript, string, ApiConfigScript>
      data={scripts}
      getId={(s) => s.name ?? ""}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => (
        <DecodesScriptEditor
          script={row}
          sensors={configSensors}
          decodeData={decodeData}
          edit={mode !== "show"}
          actions={{
            save: (script) => {
              actions.save?.(script, row.name);
              detailActions.save(script);
            },
            cancel: () => detailActions.cancel(),
          }}
        />
      )}
      renderSkeleton={() => <SCRIPT_EDITOR_PLACEHOLDER className="child-row-opening" />}
      addNew={
        edit
          ? {
              template: (id) => ({ name: `script_${Math.abs(id)}` }),
              ariaLabel: t("configs:add_script"),
            }
          : undefined
      }
      onSave={(script) => actions.save?.(script)}
      caption={t("configs:scripts")}
      tableId="configScriptsTable"
      dataTableOptions={{ stateSave: false }}
    />
  );
};

export default ConfigScriptsTable;
