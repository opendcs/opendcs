import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiDataSource, ApiDataSourceRef } from "opendcs-api";
import DataSource, { DataSourceSkeleton, type DataSourceDetails } from "./DataSource";
import type { UiDataSource } from "./DataSourceReducer";
import type { RemoveAction, SaveAction } from "../../../util/Actions";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";

export type TableDataSourceRef = Partial<ApiDataSourceRef>;

export interface DataSourcesTableProperties {
  dataSources: TableDataSourceRef[];
  getDataSource?: (dataSourceId: number) => Promise<ApiDataSource>;
  actions?: SaveAction<ApiDataSource> & RemoveAction<number>;
  loading?: boolean;
}

export const DataSourcesTable: React.FC<DataSourcesTableProperties> = ({
  dataSources,
  getDataSource,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["datasources", "translation"]);

  const columns = useMemo<ColumnDef<TableDataSourceRef>[]>(
    () => [
      {
        data: "dataSourceId",
        header: t("datasources:header.Id"),
        defaultContent: "new",
        className: "dt-left",
        type: "num",
      },
      { data: "name", header: t("datasources:header.Name"), type: "string" },
      {
        data: "type",
        header: t("datasources:header.Type"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "arguments",
        header: t("datasources:header.Arguments"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "usedBy",
        header: t("datasources:header.UsedBy"),
        defaultContent: "0",
        type: "num",
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TableDataSourceRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("datasources:edit_datasource", { id: row.dataSourceId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        show: (row) => (row.dataSourceId ?? 0) > 0,
        aria: (row) => t("datasources:delete_for", { id: row.dataSourceId }),
        onClick: ({ row }) => {
          if (row.dataSourceId !== undefined) actions.remove?.(row.dataSourceId);
        },
      },
    ],
    [t, actions],
  );

  return (
    <AppDataTable<TableDataSourceRef, number, ApiDataSource>
      data={dataSources}
      loading={loading}
      getId={(d) => d.dataSourceId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const detailsPromise: Promise<DataSourceDetails> =
          row.dataSourceId && row.dataSourceId > 0 && getDataSource
            ? getDataSource(row.dataSourceId).then((dataSource) => ({ dataSource }))
            : Promise.resolve({
                dataSource: { dataSourceId: row.dataSourceId } as UiDataSource,
              });
        return (
          <DataSource
            details={detailsPromise}
            dataSources={dataSources as ApiDataSourceRef[]}
            dataSourcesLoading={loading}
            actions={{
              save: (d) => detailActions.save(d),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <DataSourceSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ dataSourceId: id }),
        ariaLabel: t("datasources:add_datasource"),
      }}
      onSave={actions.save}
      caption={t("datasources:title")}
      tableId="dataSourcesTable"
    />
  );
};

export default DataSourcesTable;
