import { useMemo } from "react";
import { Container, Form } from "react-bootstrap";
import { renderToString } from "react-dom/server";
import type { ApiPropSpec } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useTranslation } from "react-i18next";
import type { CollectionActions } from "../../util/Actions";
import { AppDataTable, type ColumnDef } from "../data-table";

/**
 * NOTE: this should primarily be extracted from the API spec with the state value
 * added as a type extension.
 */
export interface Property {
  name: string;
  value: string;
  spec?: ApiPropSpec;
}

export interface PropertiesTableProps {
  theProps: Property[];
  edit?: boolean;
  canAdd?: boolean;
  actions: CollectionActions<Property, string>;
  width?: React.CSSProperties["width"];
  height?: React.CSSProperties["height"];
  classes?: string;
  /** Override the table caption. Defaults to the `properties:PropertiesTitle` translation. */
  caption?: React.ReactNode;
}

/** Returns the property name, falling back to the new-row counter when the name is blank. */
function propDisplayName(row: Property, rowId: string): string {
  return row.name || rowId.replace("__appdt_new_", "") || "";
}

/**
 * Properties table component for use by all pages. Control of the properties
 * by what uses the component — the save and remove methods provided should
 * behave as such.
 */
export const PropertiesTable: React.FC<PropertiesTableProps> = ({
  theProps,
  actions,
  edit = false,
  canAdd = true,
  width = "20em",
  height = "100vh",
  classes = "",
  caption,
}) => {
  const [t] = useTranslation(["properties", "translation"]);

  const columns = useMemo<ColumnDef<Property>[]>(
    () => [
      {
        data: "name",
        header: t("translation:name"),
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="text"
                name="name"
                defaultValue={row.name ?? ""}
                aria-label={t("properties:name_input", {
                  name: propDisplayName(row, rowId),
                })}
              />,
            ),
          read: (cell) =>
            cell.querySelector<HTMLInputElement>('input[name="name"]')?.value.trim() ??
            "",
        },
      },
      {
        data: "value",
        header: t("translation:value"),
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="text"
                name="value"
                defaultValue={row.value ?? ""}
                aria-label={t("properties:value_input", {
                  name: propDisplayName(row, rowId),
                })}
              />,
            ),
          read: (cell) =>
            cell.querySelector<HTMLInputElement>('input[name="value"]')?.value ?? "",
        },
      },
    ],
    [t],
  );

  return (
    <Container fluid style={{ width, height }} className={classes}>
      <AppDataTable<Property, string>
        data={theProps}
        getId={(p) => p.name}
        columns={columns}
        caption={caption ?? t("properties:PropertiesTitle")}
        actionsLabel={t("translation:actions")}
        inlineEdit={
          edit
            ? {
                onSave: (_original, updated) => {
                  if (!updated.name?.trim()) return false;
                  actions.save?.({ name: updated.name, value: updated.value ?? "" });
                },
                onAdd: (created) => {
                  if (!created.name?.trim()) return false;
                  actions.save?.({ name: created.name, value: created.value ?? "" });
                },
                onRemove: actions.remove
                  ? (row) => actions.remove!(row.name)
                  : undefined,
                newTemplate: canAdd ? () => ({ name: "", value: "" }) : undefined,
                labels: {
                  edit: (r) => t("properties:edit_prop", { name: r.name }),
                  remove: (r, rowId) =>
                    t("properties:delete_prop", { name: propDisplayName(r, rowId) }),
                  save: (r, rowId) =>
                    t("properties:save_prop", { name: propDisplayName(r, rowId) }),
                  cancel: (r, rowId) =>
                    rowId.startsWith("__appdt_new_")
                      ? t("properties:delete_prop", { name: propDisplayName(r, rowId) })
                      : t("translation:cancel"),
                  add: t("properties:add_prop"),
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
    </Container>
  );
};

export default PropertiesTable;
