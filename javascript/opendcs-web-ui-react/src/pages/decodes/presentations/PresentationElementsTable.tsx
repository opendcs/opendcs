import { useMemo } from "react";
import { Form } from "react-bootstrap";
import { renderToString } from "react-dom/server";
import { useTranslation } from "react-i18next";
import { QueryClientProvider, useQueryClient } from "@tanstack/react-query";
import type { ApiPresentationElement } from "opendcs-api";
import { AppDataTable, type ColumnDef } from "../../../components/data-table";
import UnitSelect from "../../../components/controls/UnitSelector";
import { useUnitListQuery } from "../../../queries/units";
import { useDataTypeListQuery } from "../../../queries/dataTypes";
import { elementKey } from "./PresentationReducer";

export interface PresentationElementsTableProperties {
  elements: ApiPresentationElement[];
  edit?: boolean;
  onSave: (original: ApiPresentationElement, updated: ApiPresentationElement) => void;
  onRemove: (key: string) => void;
}

const elementDisplayName = (row: ApiPresentationElement, rowId: string): string =>
  row.dataTypeCode || row.dataTypeStd || rowId.replace("__appdt_new_", "") || "";

// Parse a number cell value back to a number or undefined when blank.
const readNumber = (raw: string | undefined): number | undefined => {
  if (raw === undefined) return undefined;
  const trimmed = raw.trim();
  if (!trimmed) return undefined;
  const n = Number(trimmed);
  return Number.isFinite(n) ? n : undefined;
};

export const PresentationElementsTable: React.FC<
  PresentationElementsTableProperties
> = ({ elements, edit = false, onSave, onRemove }) => {
  const [t] = useTranslation(["presentations", "translation"]);
  const { isSuccess: unitsReady } = useUnitListQuery();
  const { data: dataTypes = [] } = useDataTypeListQuery();
  const queryClient = useQueryClient();

  const dataTypeStandards = useMemo(() => {
    const stds = new Set<string>();
    for (const dt of dataTypes) {
      if (dt.standard) stds.add(dt.standard);
    }
    // Also include any standard already on existing elements so it always appears.
    for (const el of elements) {
      if (el.dataTypeStd) stds.add(el.dataTypeStd);
    }
    return Array.from(stds).sort((a, b) => a.localeCompare(b));
  }, [dataTypes, elements]);

  const columns = useMemo<ColumnDef<ApiPresentationElement>[]>(
    () => [
      {
        data: "dataTypeStd",
        header: t("presentations:elements.dataTypeStd"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Select
                name="dataTypeStd"
                defaultValue={row.dataTypeStd ?? ""}
                aria-label={t("presentations:elements.dataTypeStd_input", {
                  name: elementDisplayName(row, rowId),
                })}
              >
                <option value="" />
                {dataTypeStandards.map((std) => (
                  <option key={std} value={std}>
                    {std}
                  </option>
                ))}
              </Form.Select>,
            ),
          read: (cell) =>
            cell.querySelector<HTMLSelectElement>('select[name="dataTypeStd"]')
              ?.value ?? "",
        },
      },
      {
        data: "dataTypeCode",
        header: t("presentations:elements.dataTypeCode"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="text"
                name="dataTypeCode"
                defaultValue={row.dataTypeCode ?? ""}
                aria-label={t("presentations:elements.dataTypeCode_input", {
                  name: elementDisplayName(row, rowId),
                })}
              />,
            ),
          read: (cell) =>
            cell
              .querySelector<HTMLInputElement>('input[name="dataTypeCode"]')
              ?.value.trim() ?? "",
        },
      },
      {
        data: "units",
        header: t("presentations:elements.units"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            unitsReady
              ? renderToString(
                  // renderToString creates an isolated React tree; UnitSelect calls
                  // useUnitListQuery, so re-provide the QueryClient explicitly.
                  <QueryClientProvider client={queryClient}>
                    <UnitSelect
                      name="units"
                      current={row.units ?? ""}
                      aria-label={t("presentations:elements.units_input", {
                        name: elementDisplayName(row, rowId),
                      })}
                    />
                  </QueryClientProvider>,
                )
              : renderToString(
                  <Form.Control
                    type="text"
                    name="units"
                    defaultValue={row.units ?? ""}
                    aria-label={t("presentations:elements.units_input", {
                      name: elementDisplayName(row, rowId),
                    })}
                  />,
                ),
          read: (cell) =>
            cell.querySelector<HTMLSelectElement | HTMLInputElement>(
              'select[name="units"], input[name="units"]',
            )?.value ?? "",
        },
      },
      {
        data: "fractionalDigits",
        header: t("presentations:elements.fractionalDigits"),
        type: "num",
        defaultContent: "",
        className: "dt-center",
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="number"
                name="fractionalDigits"
                defaultValue={row.fractionalDigits ?? ""}
                aria-label={t("presentations:elements.fractionalDigits_input", {
                  name: elementDisplayName(row, rowId),
                })}
              />,
            ),
          read: (cell) =>
            readNumber(
              cell.querySelector<HTMLInputElement>('input[name="fractionalDigits"]')
                ?.value,
            ),
        },
      },
      {
        data: "min",
        header: t("presentations:elements.min"),
        type: "num",
        defaultContent: "",
        className: "dt-center",
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="number"
                step="any"
                name="min"
                defaultValue={row.min ?? ""}
                aria-label={t("presentations:elements.min_input", {
                  name: elementDisplayName(row, rowId),
                })}
              />,
            ),
          read: (cell) =>
            readNumber(
              cell.querySelector<HTMLInputElement>('input[name="min"]')?.value,
            ),
        },
      },
      {
        data: "max",
        header: t("presentations:elements.max"),
        type: "num",
        defaultContent: "",
        className: "dt-center",
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="number"
                step="any"
                name="max"
                defaultValue={row.max ?? ""}
                aria-label={t("presentations:elements.max_input", {
                  name: elementDisplayName(row, rowId),
                })}
              />,
            ),
          read: (cell) =>
            readNumber(
              cell.querySelector<HTMLInputElement>('input[name="max"]')?.value,
            ),
        },
      },
    ],
    [t, dataTypeStandards, unitsReady, queryClient],
  );

  return (
    <AppDataTable<ApiPresentationElement, string>
      data={elements}
      getId={(e) => elementKey(e)}
      columns={columns}
      caption={t("presentations:elements.title")}
      actionsLabel={t("translation:actions")}
      inlineEdit={
        edit
          ? {
              onSave: (original, updated) => {
                if (!updated.dataTypeStd?.trim() || !updated.dataTypeCode?.trim()) {
                  return false;
                }
                onSave(original, updated);
              },
              onAdd: (created) => {
                if (!created.dataTypeStd?.trim() || !created.dataTypeCode?.trim()) {
                  return false;
                }
                onSave({}, created);
              },
              onRemove: (row) => onRemove(elementKey(row)),
              newTemplate: () => ({
                dataTypeStd: "",
                dataTypeCode: "",
                units: "",
                fractionalDigits: undefined,
                min: undefined,
                max: undefined,
              }),
              labels: {
                edit: (r, rowId) =>
                  t("presentations:elements.edit", {
                    name: elementDisplayName(r, rowId),
                  }),
                remove: (r, rowId) =>
                  t("presentations:elements.remove", {
                    name: elementDisplayName(r, rowId),
                  }),
                save: (r, rowId) =>
                  t("presentations:elements.save_edit", {
                    name: elementDisplayName(r, rowId),
                  }),
                cancel: (r, rowId) =>
                  rowId.startsWith("__appdt_new_")
                    ? t("presentations:elements.remove", {
                        name: elementDisplayName(r, rowId),
                      })
                    : t("translation:cancel"),
                add: t("presentations:elements.add"),
              },
            }
          : undefined
      }
      dataTableOptions={{
        paging: false,
        scrollY: "calc(10 * 2rem)",
        scrollCollapse: true,
      }}
      tableId="presentationElementsTable"
    />
  );
};

export default PresentationElementsTable;
