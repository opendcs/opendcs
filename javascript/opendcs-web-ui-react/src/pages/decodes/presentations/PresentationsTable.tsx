import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiPresentationGroup, ApiPresentationRef } from "opendcs-api";
import Presentation, {
  PresentationSkeleton,
  type PresentationDetails,
} from "./Presentation";
import type { UiPresentation } from "./PresentationReducer";
import type { RemoveAction, SaveAction } from "../../../util/Actions";
import {
  AppDataTable,
  dateColumn,
  idColumn,
  textColumn,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";

export type TablePresentationRef = Partial<ApiPresentationRef>;

export interface PresentationsTableProperties {
  presentations: TablePresentationRef[];
  getPresentation?: (groupId: number) => Promise<ApiPresentationGroup>;
  actions?: SaveAction<ApiPresentationGroup> & RemoveAction<number>;
  loading?: boolean;
}

export const PresentationsTable: React.FC<PresentationsTableProperties> = ({
  presentations,
  getPresentation,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["presentations", "translation"]);

  const columns = useMemo<ColumnDef<TablePresentationRef>[]>(
    () => [
      idColumn("groupId", t("presentations:header.Id")),
      {
        data: "name",
        header: t("presentations:header.Name"),
        type: "string",
        defaultSort: "asc",
      },
      textColumn("inheritsFrom", t("presentations:header.InheritsFrom")),
      {
        data: "production",
        header: t("presentations:header.Production"),
        defaultContent: "",
        className: "dt-center",
        type: "num",
        render: (data: unknown, type: string) => {
          const production = Boolean(data);
          if (type !== "display") return production ? 1 : 0;
          return production ? "✓" : "";
        },
      },
      dateColumn("lastModified", t("presentations:header.LastModified")),
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TablePresentationRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("presentations:edit_presentation", { id: row.groupId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        confirm: true,
        show: (row) => (row.groupId ?? 0) > 0,
        aria: (row) => t("presentations:delete_for", { id: row.groupId }),
        onClick: ({ row }) => {
          if (row.groupId !== undefined) actions.remove?.(row.groupId);
        },
      },
    ],
    [t, actions],
  );

  return (
    <AppDataTable<TablePresentationRef, number, ApiPresentationGroup>
      data={presentations}
      loading={loading}
      getId={(p) => p.groupId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const detailsPromise: Promise<PresentationDetails> =
          row.groupId && row.groupId > 0 && getPresentation
            ? getPresentation(row.groupId).then((presentation) => ({ presentation }))
            : Promise.resolve({
                presentation: { groupId: row.groupId } as UiPresentation,
              });
        return (
          <Presentation
            details={detailsPromise}
            actions={{
              save: (p) => detailActions.save(p),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <PresentationSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ groupId: id, production: false }),
        ariaLabel: t("presentations:add_presentation"),
      }}
      onSave={actions.save}
      caption={t("presentations:title")}
      tableId="presentationsTable"
    />
  );
};

export default PresentationsTable;
