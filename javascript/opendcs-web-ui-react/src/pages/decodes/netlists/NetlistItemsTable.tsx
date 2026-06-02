import { useMemo } from "react";
import { Form } from "react-bootstrap";
import { renderToString } from "react-dom/server";
import { useTranslation } from "react-i18next";
import type { ApiNetListItem } from "opendcs-api";
import { AppDataTable, type ColumnDef } from "../../../components/data-table";

export interface NetlistItemsTableProperties {
  items: ApiNetListItem[];
  edit?: boolean;
  onSave: (item: ApiNetListItem) => void;
  onRemove: (transportId: string) => void;
}

const ROW_ID = (item: ApiNetListItem) => item.transportId ?? "";

const itemDisplayName = (row: ApiNetListItem, rowId: string): string =>
  row.transportId || rowId.replace("__appdt_new_", "") || "";

export const NetlistItemsTable: React.FC<NetlistItemsTableProperties> = ({
  items,
  edit = false,
  onSave,
  onRemove,
}) => {
  const [t] = useTranslation(["netlists", "translation"]);

  const columns = useMemo<ColumnDef<ApiNetListItem>[]>(
    () => [
      {
        data: "transportId",
        header: t("netlists:items.transportId"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="text"
                name="transportId"
                defaultValue={row.transportId ?? ""}
                aria-label={t("netlists:items.transportId_input", {
                  name: itemDisplayName(row, rowId),
                })}
              />,
            ),
          read: (cell) =>
            cell
              .querySelector<HTMLInputElement>('input[name="transportId"]')
              ?.value.trim() ?? "",
        },
      },
      {
        data: "platformName",
        header: t("netlists:items.platformName"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="text"
                name="platformName"
                defaultValue={row.platformName ?? ""}
                aria-label={t("netlists:items.platformName_input", {
                  name: itemDisplayName(row, rowId),
                })}
              />,
            ),
          read: (cell) =>
            cell.querySelector<HTMLInputElement>('input[name="platformName"]')?.value ??
            "",
        },
      },
      {
        data: "description",
        header: t("netlists:items.description"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="text"
                name="description"
                defaultValue={row.description ?? ""}
                aria-label={t("netlists:items.description_input", {
                  name: itemDisplayName(row, rowId),
                })}
              />,
            ),
          read: (cell) =>
            cell.querySelector<HTMLInputElement>('input[name="description"]')?.value ??
            "",
        },
      },
    ],
    [t],
  );

  return (
    <AppDataTable<ApiNetListItem, string>
      data={items}
      getId={ROW_ID}
      columns={columns}
      caption={t("netlists:items.title")}
      actionsLabel={t("translation:actions")}
      inlineEdit={
        edit
          ? {
              onSave: (_original, updated) => {
                if (!updated.transportId?.trim()) return false;
                onSave({
                  transportId: updated.transportId.trim(),
                  platformName: updated.platformName ?? "",
                  description: updated.description ?? "",
                });
              },
              onAdd: (created) => {
                if (!created.transportId?.trim()) return false;
                onSave({
                  transportId: created.transportId.trim(),
                  platformName: created.platformName ?? "",
                  description: created.description ?? "",
                });
              },
              onRemove: (row) => {
                if (row.transportId) onRemove(row.transportId);
              },
              newTemplate: () => ({
                transportId: "",
                platformName: "",
                description: "",
              }),
              labels: {
                edit: (r, rowId) =>
                  t("netlists:items.edit", {
                    transportId: itemDisplayName(r, rowId),
                  }),
                remove: (r, rowId) =>
                  t("netlists:items.remove", {
                    transportId: itemDisplayName(r, rowId),
                  }),
                save: (r, rowId) =>
                  t("netlists:items.save_edit", {
                    transportId: itemDisplayName(r, rowId),
                  }),
                cancel: (r, rowId) =>
                  rowId.startsWith("__appdt_new_")
                    ? t("netlists:items.remove", {
                        transportId: itemDisplayName(r, rowId),
                      })
                    : t("translation:cancel"),
                add: t("netlists:items.add"),
              },
            }
          : undefined
      }
      dataTableOptions={{
        paging: false,
        scrollY: "calc(10 * 2rem)",
        scrollCollapse: true,
      }}
      tableId="netlistItemsTable"
    />
  );
};

export default NetlistItemsTable;
