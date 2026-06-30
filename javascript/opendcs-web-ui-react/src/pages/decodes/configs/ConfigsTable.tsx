import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiConfigRef, ApiDecodedMessage, ApiPlatformConfig } from "opendcs-api";
import Config, { ConfigSkeleton } from "./Config";
import type { UiConfig } from "./ConfigReducer";
import type { ConfigDetails } from "./Config";
import type { RemoveAction, SaveAction } from "../../../util/Actions";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";

export type TableConfigRef = Partial<ApiConfigRef>;

export interface ConfigsTableProperties {
  configs: TableConfigRef[];
  getConfig?: (configId: number) => Promise<ApiPlatformConfig>;
  decodeData?: (raw: string) => ApiDecodedMessage;
  actions?: SaveAction<ApiPlatformConfig> & RemoveAction<number>;
  loading?: boolean;
}

export const ConfigsTable: React.FC<ConfigsTableProperties> = ({
  configs,
  getConfig,
  decodeData,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["configs", "translation"]);

  const columns = useMemo<ColumnDef<TableConfigRef>[]>(
    () => [
      {
        data: "configId",
        header: t("configs:header.Id"),
        defaultContent: "new",
        className: "dt-left",
        type: "num",
      },
      { data: "name", header: t("configs:header.Name"), type: "string" },
      {
        data: "numPlatforms",
        header: t("configs:header.NumPlatforms"),
        defaultContent: "0",
        type: "num",
      },
      {
        data: "description",
        header: t("configs:header.Description"),
        defaultContent: "",
        type: "string",
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TableConfigRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("configs:edit_config", { id: row.configId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        show: (row) => (row.configId ?? 0) > 0,
        aria: (row) => t("configs:delete_for", { id: row.configId }),
        confirm: () => t("translation:confirm_delete_prompt"),
        onClick: ({ row }) => {
          if (row.configId !== undefined) actions.remove?.(row.configId);
        },
      },
    ],
    [t, actions],
  );

  return (
    <AppDataTable<TableConfigRef, number, ApiPlatformConfig>
      data={configs}
      loading={loading}
      getId={(c) => c.configId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const detailsPromise: Promise<ConfigDetails> =
          row.configId && row.configId > 0 && getConfig
            ? getConfig(row.configId).then((config) => ({ config }))
            : Promise.resolve({
                config: { configId: row.configId } as UiConfig,
              });
        return (
          <Config
            details={detailsPromise}
            actions={{
              save: (c) => detailActions.save(c),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
            decodeData={decodeData}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <ConfigSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ configId: id }),
        ariaLabel: t("configs:add_config"),
      }}
      onSave={actions.save}
      caption={t("configs:configsTitle")}
      tableId="configsTable"
    />
  );
};

export default ConfigsTable;
