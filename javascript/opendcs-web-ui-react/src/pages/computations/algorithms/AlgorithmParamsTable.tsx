import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Form } from "react-bootstrap";
import { renderToString } from "react-dom/server";
import { PARM_TYPES, parmTypeLabel, type ParmTypeOption } from "../common/parmTypes";
import { AppDataTable, type ColumnDef } from "../../../components/data-table";

export type AlgoParm = { roleName: string; parmType: string };
// Preserve the legacy re-exports from this module; callers import PARM_TYPES /
// parmTypeLabel from here rather than from `common/parmTypes` directly.
// eslint-disable-next-line react-refresh/only-export-components
export { PARM_TYPES, parmTypeLabel };
export type { ParmTypeOption };

export interface AlgorithmParamsTableProps {
  parms: AlgoParm[];
  edit?: boolean;
  onAdd?: (parm: AlgoParm) => void;
  onRemove?: (roleName: string) => void;
  onUpdate?: (oldRoleName: string, newParm: AlgoParm) => void;
}

export const AlgorithmParamsTable: React.FC<AlgorithmParamsTableProps> = ({
  parms,
  edit = false,
  onAdd,
  onRemove,
  onUpdate,
}) => {
  const [t] = useTranslation(["algorithms", "translation"]);

  const columns = useMemo<ColumnDef<AlgoParm>[]>(
    () => [
      {
        data: "roleName",
        header: t("algorithms:parms.roleName"),
        edit: {
          render: (row) =>
            renderToString(
              <Form.Control
                type="text"
                name="roleName"
                defaultValue={row.roleName}
                aria-label={t("algorithms:parms.roleName_input")}
              />,
            ),
          read: (cell) =>
            cell
              .querySelector<HTMLInputElement>('input[name="roleName"]')
              ?.value.trim() ?? "",
        },
      },
      {
        data: "parmType",
        header: t("algorithms:parms.parmType"),
        render: (_data, type, row) =>
          type === "display" ? parmTypeLabel(row.parmType ?? "") : (row.parmType ?? ""),
        edit: {
          render: (row) =>
            renderToString(
              <Form.Select name="parmType" defaultValue={row.parmType ?? "i"}>
                {PARM_TYPES.map((pt) => (
                  <option key={pt.value} value={pt.value}>
                    {pt.label}
                  </option>
                ))}
              </Form.Select>,
            ),
          read: (cell) =>
            cell.querySelector<HTMLSelectElement>('select[name="parmType"]')?.value ??
            "i",
        },
      },
    ],
    [t],
  );

  return (
    <AppDataTable<AlgoParm, string>
      data={parms}
      getId={(p) => p.roleName}
      columns={columns}
      caption={t("algorithms:parms.title")}
      actionsLabel={t("translation:actions")}
      inlineEdit={
        edit
          ? {
              onSave: (original, updated) => {
                if (!updated.roleName) return false;
                onUpdate?.(original.roleName, updated);
              },
              onAdd: (created) => {
                if (!created.roleName) return false;
                onAdd?.(created);
              },
              onRemove: (row) => onRemove?.(row.roleName),
              newTemplate: () => ({ roleName: "", parmType: "i" }),
              labels: {
                edit: (r) => t("algorithms:parms.edit_for", { name: r.roleName }),
                remove: (r) => t("algorithms:parms.delete_for", { name: r.roleName }),
                save: () => t("algorithms:parms.save_parm"),
                cancel: () => t("algorithms:parms.cancel_parm"),
                add: t("algorithms:parms.add_parm"),
              },
            }
          : undefined
      }
      dataTableOptions={{
        paging: false,
        scrollY: "calc(10 * 2rem)",
        scrollCollapse: true,
      }}
    />
  );
};
