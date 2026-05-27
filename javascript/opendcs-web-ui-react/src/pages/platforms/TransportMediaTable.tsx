import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiTransportMedium } from "opendcs-api";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../components/data-table";
import type { RemoveAction } from "../../util/Actions";
import TransportMedium, {
  TransportMediumSkeleton,
  type UiTransportMedium,
} from "./TransportMedium";
import { transportKey } from "./transportKey";

export interface TransportMediaTableProperties {
  media: UiTransportMedium[];
  actions?: {
    save?: (medium: ApiTransportMedium, originalKey?: string) => void;
  } & RemoveAction<string>;
  /** Edit-mode of the parent Platform detail card. When false, row actions are hidden. */
  edit?: boolean;
}

export const TransportMediaTable: React.FC<TransportMediaTableProperties> = ({
  media,
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["platforms", "translation"]);

  const columns = useMemo<ColumnDef<UiTransportMedium>[]>(
    () => [
      {
        data: "mediumType",
        header: t("platforms:medium_type"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "mediumId",
        header: t("platforms:medium_id"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "scriptName",
        header: t("platforms:script_name"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "channelNum",
        header: t("platforms:channel_num"),
        defaultContent: "",
        type: "num",
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<UiTransportMedium>[]>(
    () =>
      edit
        ? [
            {
              key: "edit",
              icon: "bi-pencil",
              variant: "warning",
              aria: (row) => t("platforms:edit_transport", { id: row.mediumId ?? "" }),
              onClick: ({ row, api }) => api.setMode(row, "edit"),
            },
            {
              key: "delete",
              icon: "bi-trash",
              variant: "danger",
              aria: (row) =>
                t("platforms:delete_transport_for", { id: row.mediumId ?? "" }),
              onClick: ({ row }) => {
                actions.remove?.(transportKey(row));
              },
            },
          ]
        : [],
    [edit, t, actions],
  );

  return (
    <AppDataTable<UiTransportMedium, string, ApiTransportMedium>
      data={media}
      getId={(m) => transportKey(m)}
      columns={columns}
      actionsLabel={edit ? t("translation:actions") : undefined}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => (
        <TransportMedium
          medium={row}
          edit={mode !== "show"}
          originalKey={transportKey(row)}
          actions={{
            save: (medium, originalKey) => {
              actions.save?.(medium, originalKey);
              detailActions.save(medium);
            },
            cancel: () => detailActions.cancel(),
          }}
        />
      )}
      renderSkeleton={({ mode }) => (
        <TransportMediumSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={
        edit
          ? {
              template: (id) => ({
                mediumType: "GOES",
                mediumId: `new-${Math.abs(id)}`,
              }),
              ariaLabel: t("platforms:add_transport"),
            }
          : undefined
      }
      caption={t("platforms:transport_media")}
      tableId="transportMediaTable"
      dataTableOptions={{ stateSave: false }}
    />
  );
};

export default TransportMediaTable;
