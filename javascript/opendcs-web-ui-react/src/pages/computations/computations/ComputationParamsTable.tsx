import { useContext, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Form } from "react-bootstrap";
import { renderToString } from "react-dom/server";
import { QueryClientProvider, useQueryClient } from "@tanstack/react-query";
import {
  TimeSeriesMethodsIntervalMethodsApi,
  type ApiCompParm,
  type ApiInterval,
} from "opendcs-api";
import { PARM_TYPES, parmTypeLabel } from "../common/parmTypes";
import { useApi } from "../../../contexts/app/ApiContext";
import UnitSelect from "../../../components/controls/UnitSelector";
import { useUnitListQuery } from "../../../queries/units";
import ComputationParamsOptionsContext from "../../../contexts/data/ComputationParamsOptionsContext";
import { AppDataTable, type ColumnDef } from "../../../components/data-table";

export type CompParm = ApiCompParm;

const readOptional = (cell: HTMLElement, selector: string): string | undefined => {
  const el = cell.querySelector<HTMLInputElement | HTMLSelectElement>(selector);
  const val = el?.value.trim() ?? "";
  return val.length > 0 ? val : undefined;
};

export interface ComputationParamsTableProps {
  parms: CompParm[];
  edit?: boolean;
  onAdd?: (parm: CompParm) => void;
  onRemove?: (algoRoleName: string) => void;
  onUpdate?: (oldRoleName: string, newParm: CompParm) => void;
}

export const ComputationParamsTable: React.FC<ComputationParamsTableProps> = ({
  parms,
  edit = false,
  onAdd,
  onRemove,
  onUpdate,
}) => {
  const [t] = useTranslation(["computations", "translation"]);
  const api = useApi();
  const { isSuccess: unitsReady } = useUnitListQuery();
  // renderToString below creates an isolated React tree; UnitSelect inside it
  // calls useUnitListQuery, so we must re-provide the QueryClient explicitly.
  const queryClient = useQueryClient();
  const { deltaTUnits, ifMissingActions, defaultIntervals } = useContext(
    ComputationParamsOptionsContext,
  );
  const intervalApi = useMemo(
    () => new TimeSeriesMethodsIntervalMethodsApi(api.conf),
    [api.conf],
  );
  const [intervalNames, setIntervalNames] = useState<string[]>([]);

  useEffect(() => {
    if (!api.org) return;
    let cancelled = false;
    intervalApi
      .getIntervals(api.org)
      .then((intervals: ApiInterval[]) => {
        if (cancelled) return;
        setIntervalNames(
          intervals.map((i) => i.name ?? "").filter((n) => n.length > 0),
        );
      })
      .catch(() => {
        if (cancelled) return;
        setIntervalNames([]);
      });
    return () => {
      cancelled = true;
    };
  }, [api.org, intervalApi]);

  const intervalOptions = useMemo(() => {
    const opts = new Set<string>(defaultIntervals);
    intervalNames.forEach((n) => opts.add(n));
    parms
      .map((p) => p.interval ?? "")
      .filter((v) => v.length > 0)
      .forEach((v) => opts.add(v));
    return Array.from(opts).sort((a, b) => a.localeCompare(b));
  }, [defaultIntervals, intervalNames, parms]);

  const columns = useMemo<ColumnDef<CompParm>[]>(() => {
    const combobox = (
      name: string,
      defaultValue: string,
      ariaLabel: string,
      rowId: string,
      options?: string[],
    ): string => {
      const listId = `${name}-${rowId}-opts`;
      return renderToString(
        <>
          <input
            type="text"
            name={name}
            defaultValue={defaultValue}
            list={listId}
            className="form-control form-control-sm"
            aria-label={ariaLabel}
          />
          <datalist id={listId}>
            {(options ?? []).map((opt) => (
              <option key={opt} value={opt} />
            ))}
          </datalist>
        </>,
      );
    };

    return [
      {
        data: "algoRoleName",
        header: t("computations:parms.roleName"),
        type: "string",
        edit: {
          render: (row) =>
            renderToString(
              <Form.Control
                type="text"
                name="algoRoleName"
                defaultValue={row.algoRoleName ?? ""}
                size="sm"
                aria-label={t("computations:parms.roleName_input")}
              />,
            ),
          read: (cell) =>
            cell.querySelector<HTMLInputElement>("input")?.value.trim() ?? "",
        },
      },
      {
        data: "algoParmType",
        header: t("computations:parms.parmType"),
        type: "string",
        render: (data: unknown) => parmTypeLabel(typeof data === "string" ? data : ""),
        edit: {
          render: (row) =>
            renderToString(
              <Form.Select
                name="algoParmType"
                defaultValue={row.algoParmType ?? "i"}
                size="sm"
                aria-label={t("computations:parms.parmType_input")}
              >
                {PARM_TYPES.map((pt) => (
                  <option key={pt.value} value={pt.value}>
                    {pt.label}
                  </option>
                ))}
              </Form.Select>,
            ),
          read: (cell) => cell.querySelector<HTMLSelectElement>("select")?.value ?? "i",
        },
      },
      {
        data: "siteName",
        header: t("computations:parms.site"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            combobox(
              "siteName",
              row.siteName ?? "",
              t("computations:parms.site_input"),
              rowId,
            ),
          read: (cell) => readOptional(cell, "input"),
        },
      },
      {
        data: "dataType",
        header: t("computations:parms.dataType"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            combobox(
              "dataType",
              row.dataType ?? "",
              t("computations:parms.dataType_input"),
              rowId,
            ),
          read: (cell) => readOptional(cell, "input"),
        },
      },
      {
        data: "interval",
        header: t("computations:parms.interval"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            combobox(
              "interval",
              row.interval ?? "",
              t("computations:parms.interval_input"),
              rowId,
              intervalOptions,
            ),
          read: (cell) => readOptional(cell, "input"),
        },
      },
      {
        data: "paramType",
        header: t("computations:parms.paramType"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            combobox(
              "paramType",
              row.paramType ?? "",
              t("computations:parms.paramType_input"),
              rowId,
            ),
          read: (cell) => readOptional(cell, "input"),
        },
      },
      {
        data: "duration",
        header: t("computations:parms.duration"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            combobox(
              "duration",
              row.duration ?? "",
              t("computations:parms.duration_input"),
              rowId,
            ),
          read: (cell) => readOptional(cell, "input"),
        },
      },
      {
        data: "version",
        header: t("computations:parms.version"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            combobox(
              "version",
              row.version ?? "",
              t("computations:parms.version_input"),
              rowId,
            ),
          read: (cell) => readOptional(cell, "input"),
        },
      },
      {
        data: "deltaT",
        header: t("computations:parms.deltaT"),
        type: "num",
        defaultContent: "0",
        edit: {
          render: (row) =>
            renderToString(
              <Form.Control
                type="number"
                name="deltaT"
                defaultValue={row.deltaT ?? 0}
                size="sm"
                aria-label={t("computations:parms.deltaT_input")}
              />,
            ),
          read: (cell) => {
            const val = cell.querySelector<HTMLInputElement>("input")?.value ?? "0";
            const n = Number.parseInt(val, 10);
            return Number.isNaN(n) ? 0 : n;
          },
        },
      },
      {
        data: "deltaTUnits",
        header: t("computations:parms.deltaTUnits"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row) =>
            renderToString(
              <Form.Select
                name="deltaTUnits"
                defaultValue={row.deltaTUnits ?? "Seconds"}
                size="sm"
                aria-label={t("computations:parms.deltaTUnits_input")}
              >
                {deltaTUnits.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </Form.Select>,
            ),
          read: (cell) =>
            cell.querySelector<HTMLSelectElement>("select")?.value ?? "Seconds",
        },
      },
      {
        data: "unitsAbbr",
        header: t("computations:parms.units"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row) =>
            unitsReady
              ? renderToString(
                  // renderToString creates an isolated React tree; UnitSelect calls
                  // useUnitListQuery, so re-provide the QueryClient explicitly.
                  <QueryClientProvider client={queryClient}>
                    <UnitSelect
                      size="sm"
                      name="unitsAbbr"
                      current={row.unitsAbbr ?? ""}
                      aria-label={t("computations:parms.units_input")}
                    />
                  </QueryClientProvider>,
                )
              : renderToString(
                  <Form.Control
                    type="text"
                    size="sm"
                    name="unitsAbbr"
                    defaultValue={row.unitsAbbr ?? ""}
                    aria-label={t("computations:parms.units_input")}
                  />,
                ),
          read: (cell) => readOptional(cell, "select, input"),
        },
      },
      {
        data: "ifMissing",
        header: t("computations:parms.ifMissing"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row) =>
            renderToString(
              <Form.Select
                name="ifMissing"
                defaultValue={row.ifMissing ?? "FAIL"}
                size="sm"
                aria-label={t("computations:parms.ifMissing_input")}
              >
                {ifMissingActions.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </Form.Select>,
            ),
          read: (cell) =>
            cell.querySelector<HTMLSelectElement>("select")?.value ?? "FAIL",
        },
      },
    ];
  }, [t, intervalOptions, deltaTUnits, ifMissingActions, unitsReady, queryClient]);

  const newTemplate = (): CompParm => ({
    algoParmType: "i",
    deltaT: 0,
    deltaTUnits: "Seconds",
    ifMissing: "FAIL",
  });

  return (
    <AppDataTable<CompParm, string>
      data={parms}
      getId={(p) => p.algoRoleName ?? ""}
      columns={columns}
      actionsLabel={t("translation:actions")}
      inlineEdit={
        edit
          ? {
              onSave: (original, updated) => {
                if (!(updated.algoRoleName ?? "").trim()) return false;
                onUpdate?.(original.algoRoleName ?? "", updated);
              },
              onAdd: (created) => {
                if (!(created.algoRoleName ?? "").trim()) return false;
                onAdd?.(created);
              },
              onRemove: (row) => onRemove?.(row.algoRoleName ?? ""),
              newTemplate,
              labels: {
                edit: (row) =>
                  t("computations:parms.edit_for", { name: row.algoRoleName }),
                remove: (row) =>
                  t("computations:parms.delete_for", { name: row.algoRoleName }),
                save: () => t("computations:parms.save_parm"),
                cancel: () => t("computations:parms.cancel_parm"),
                add: t("computations:parms.add_parm"),
              },
            }
          : undefined
      }
      caption={t("computations:parms.title")}
    />
  );
};
