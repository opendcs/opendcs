import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiPlatform, ApiPlatformRef } from "opendcs-api";
import Platform, { PlatformSkeleton, type UiPlatform } from "./Platform";
import type { RemoveAction, SaveAction } from "../../util/Actions";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../components/data-table";

export type TablePlatformRef = Partial<ApiPlatformRef>;

export interface PlatformsTableProperties {
  platforms: TablePlatformRef[];
  getPlatform?: (platformId: number) => Promise<ApiPlatform>;
  actions?: SaveAction<ApiPlatform> & RemoveAction<number>;
  loading?: boolean;
}

export const PlatformsTable: React.FC<PlatformsTableProperties> = ({
  platforms,
  getPlatform,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["platforms", "translation"]);

  const columns = useMemo<ColumnDef<TablePlatformRef>[]>(
    () => [
      {
        data: "platformId",
        header: t("platforms:header.Id"),
        defaultContent: "new",
        className: "dt-left",
        type: "num",
      },
      { data: "name", header: t("platforms:header.Platform"), type: "string" },
      {
        data: "agency",
        header: t("platforms:header.Agency"),
        defaultContent: "",
        type: "string",
      },
      {
        data: null,
        header: t("platforms:header.TransportId"),
        defaultContent: "",
        render: (_data, _type, row) =>
          row.transportMedia ? Object.values(row.transportMedia).join(", ") : "",
      },
      {
        data: "config",
        header: t("platforms:header.Config"),
        defaultContent: "",
        type: "string",
      },
      {
        data: "description",
        header: t("platforms:header.Description"),
        defaultContent: "",
        type: "string",
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TablePlatformRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("platforms:edit_platform", { id: row.platformId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        show: (row) => (row.platformId ?? 0) > 0,
        aria: (row) => t("platforms:delete_for", { id: row.platformId }),
        onClick: ({ row }) => {
          if (row.platformId !== undefined) actions.remove?.(row.platformId);
        },
      },
    ],
    [t, actions],
  );

  return (
    <AppDataTable<TablePlatformRef, number, ApiPlatform>
      data={platforms}
      loading={loading}
      getId={(p) => p.platformId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const platformPromise: Promise<UiPlatform> =
          row.platformId && row.platformId > 0 && getPlatform
            ? getPlatform(row.platformId)
            : Promise.resolve({ platformId: row.platformId } as UiPlatform);
        return (
          <Platform
            platform={platformPromise}
            actions={{
              save: (p) => detailActions.save(p),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <PlatformSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ platformId: id }),
        ariaLabel: t("platforms:add_platform"),
      }}
      onSave={actions.save}
      caption={t("platforms:platformsTitle")}
      tableId="platformTable"
    />
  );
};
