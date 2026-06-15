import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiEquipmentModel, ApiEquipmentModelRef } from "opendcs-api";
import Equipment, { EquipmentSkeleton, type UiEquipmentModel } from "./Equipment";
import type { RemoveAction, SaveAction } from "../../util/Actions";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../components/data-table";

export type TableEquipmentRef = Partial<ApiEquipmentModelRef>;

export interface EquipmentTableProperties {
  equipment: TableEquipmentRef[];
  getEquipment?: (equipmentId: number) => Promise<ApiEquipmentModel>;
  actions?: SaveAction<ApiEquipmentModel> & RemoveAction<number>;
  loading?: boolean;
}

export const EquipmentTable: React.FC<EquipmentTableProperties> = ({
  equipment,
  getEquipment,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["equipment", "translation"]);

  const columns = useMemo<ColumnDef<TableEquipmentRef>[]>(
    () => [
      {
        data: "equipmentId",
        header: t("equipment:equipment_id"),
        defaultContent: "new",
        type: "num",
      },
      { data: "name", header: t("equipment:name"), type: "string" },
      {
        data: "equipmentType",
        header: t("equipment:equipment_type"),
        type: "string",
      },
      { data: "company", header: t("equipment:company"), type: "string" },
      { data: "model", header: t("equipment:model"), type: "string" },
      { data: "description", header: t("equipment:description"), type: "string" },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TableEquipmentRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("equipment:edit_equipment", { id: row.equipmentId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        show: (row) => (row.equipmentId ?? 0) > 0,
        aria: (row) => t("equipment:delete_for", { id: row.equipmentId }),
        onClick: ({ row }) => {
          if (row.equipmentId !== undefined) actions.remove?.(row.equipmentId);
        },
      },
    ],
    [t, actions],
  );

  return (
    <AppDataTable<TableEquipmentRef, number, ApiEquipmentModel>
      data={equipment}
      loading={loading}
      getId={(e) => e.equipmentId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const equipmentPromise: Promise<UiEquipmentModel> =
          row.equipmentId && row.equipmentId > 0 && getEquipment
            ? getEquipment(row.equipmentId)
            : Promise.resolve({ equipmentId: row.equipmentId } as UiEquipmentModel);
        return (
          <Equipment
            equipment={equipmentPromise}
            actions={{
              save: (eq) => detailActions.save(eq),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
          />
        );
      }}
      renderSkeleton={({ mode }) => (
        <EquipmentSkeleton edit={mode !== "show"} className="child-row-opening" />
      )}
      addNew={{
        template: (id) => ({ equipmentId: id }),
        ariaLabel: t("equipment:add_equipment"),
      }}
      onSave={actions.save}
      caption={t("equipment:title")}
      tableId="equipmentTable"
    />
  );
};
