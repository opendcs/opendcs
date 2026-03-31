import DataTable, {
  type DataTableProps,
  type DataTableRef,
  type DataTableSlots,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5";
import { useTranslation } from "react-i18next";
import { useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { dtLangs } from "../../../lang";
import { Button, Form } from "react-bootstrap";
import { ChevronRight, Pencil, Save, Trash, X } from "react-bootstrap-icons";
import { renderToString } from "react-dom/server";
import {
  TimeSeriesMethodsIntervalMethodsApi,
  type ApiCompParm,
  type ApiInterval,
} from "opendcs-api";
import { PARM_TYPES, parmTypeLabel, type ParmTypeOption } from "../common/parmTypes";
import { useApi } from "../../../contexts/app/ApiContext";
import UnitSelect from "../../../components/controls/UnitSelector";
import UnitsContext, {
  defaultValue as defaultUnitsContext,
} from "../../../contexts/data/UnitsContext";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(dtButtons);

export type CompParm = ApiCompParm;
export { PARM_TYPES, parmTypeLabel };
export type { ParmTypeOption };

const DELTA_T_UNITS = [
  "Seconds",
  "Minutes",
  "Hours",
  "Days",
  "Weeks",
  "Months",
  "Years",
];
const IF_MISSING_ACTIONS = ["FAIL", "IGNORE", "PREV", "NEXT", "INTERP", "CLOSEST"];
const DEFAULT_INTERVALS = [
  "0",
  "1Minute",
  "5Minutes",
  "10Minutes",
  "15Minutes",
  "30Minutes",
  "1Hour",
  "2Hours",
  "3Hours",
  "6Hours",
  "12Hours",
  "1Day",
  "1Week",
  "1Month",
  "1Year",
];

type ExistingRow = {
  kind: "existing";
  parm: CompParm;
  editing: boolean;
};
type NewRow = { kind: "new"; idx: number; parm: CompParm };
type RowParm = ExistingRow | NewRow;

type RowValues = {
  algoRoleName: string;
  algoParmType: string;
  siteName?: string;
  dataType?: string;
  interval?: string;
  paramType?: string;
  duration?: string;
  version?: string;
  deltaT?: string;
  deltaTUnits?: string;
  unitsAbbr?: string;
  ifMissing?: string;
  roleInput: HTMLInputElement | null;
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
  const [t, i18n] = useTranslation(["computations", "translation"]);
  const api = useApi();
  const units = useContext(UnitsContext) ?? defaultUnitsContext;
  const table = useRef<DataTableRef>(null);
  const intervalApi = useMemo(
    () => new TimeSeriesMethodsIntervalMethodsApi(api.conf),
    [api.conf],
  );
  const [intervalNames, setIntervalNames] = useState<string[]>([]);
  const [newParms, setNewParms] = useState<NewRow[]>([]);
  const [editingNames, setEditingNames] = useState<Set<string>>(new Set());
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
  const nextIdx = useRef(0);

  const isEditing = useCallback(
    (data: RowParm) =>
      data.kind === "new" || (data.kind === "existing" && data.editing),
    [],
  );

  const rowKey = useCallback((data: RowParm): string => {
    if (data.kind === "new") return `new:${data.idx}`;
    return `existing:${(data.parm.algoRoleName ?? "").trim().toLowerCase()}`;
  }, []);

  const allRows = useMemo<RowParm[]>(
    () => [
      ...parms.map(
        (p): ExistingRow => ({
          kind: "existing",
          parm: p,
          editing: editingNames.has(p.algoRoleName ?? ""),
        }),
      ),
      ...newParms,
    ],
    [parms, newParms, editingNames],
  );

  useEffect(() => {
    if (!api.org) {
      setIntervalNames([]);
      return;
    }
    let cancelled = false;
    intervalApi
      .getIntervals(api.org)
      .then((intervals: ApiInterval[]) => {
        if (cancelled) return;
        const names = intervals
          .map((interval) => interval.name ?? "")
          .filter((name) => name.length > 0);
        setIntervalNames(names);
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
    const options = new Set<string>(DEFAULT_INTERVALS);
    intervalNames.forEach((name) => options.add(name));
    parms
      .map((parm) => parm.interval ?? "")
      .filter((interval) => interval.length > 0)
      .forEach((interval) => options.add(interval));
    return Array.from(options).sort((a, b) => a.localeCompare(b));
  }, [intervalNames, parms]);

  useEffect(() => {
    const valid = new Set(allRows.map((row) => rowKey(row)));
    setExpandedRows((prev) => new Set([...prev].filter((key) => valid.has(key))));
  }, [allRows, rowKey]);

  const findRowNode = useCallback(
    (predicate: (data: RowParm) => boolean): HTMLTableRowElement | null => {
      const dt = table.current?.dt();
      if (!dt) return null;
      let node: HTMLTableRowElement | null = null;
      dt.rows().every(function () {
        if (predicate(this.data() as RowParm)) {
          node = this.node() as HTMLTableRowElement;
        }
      });
      return node;
    },
    [],
  );

  const findChildNode = (rowNode: HTMLTableRowElement): HTMLElement | null => {
    const sibling = rowNode.nextElementSibling as HTMLElement | null;
    if (!sibling || !sibling.classList.contains("child")) return null;
    return sibling;
  };

  const readRowValues = (rowNode: HTMLTableRowElement): RowValues => {
    const childNode = findChildNode(rowNode);
    const query = <T extends Element>(selector: string): T | null =>
      (rowNode.querySelector(selector) as T | null) ??
      (childNode?.querySelector(selector) as T | null) ??
      null;

    const readOptionalText = (selector: string): string | undefined => {
      const input = query<HTMLInputElement>(selector);
      if (!input) return undefined;
      return input.value.trim();
    };

    const readOptionalSelect = (selector: string): string | undefined => {
      const select = query<HTMLSelectElement>(selector);
      if (!select) return undefined;
      return select.value;
    };

    const roleInput = query<HTMLInputElement>('input[name="algoRoleName"]');

    return {
      algoRoleName: roleInput?.value.trim() ?? "",
      algoParmType:
        query<HTMLSelectElement>('select[name="algoParmType"]')?.value ?? "i",
      siteName: readOptionalText('input[name="siteName"]'),
      dataType: readOptionalText('input[name="dataType"]'),
      interval: readOptionalText('input[name="interval"]'),
      paramType: readOptionalText('input[name="paramType"]'),
      duration: readOptionalText('input[name="duration"]'),
      version: readOptionalText('input[name="version"]'),
      deltaT: readOptionalText('input[name="deltaT"]'),
      deltaTUnits: readOptionalSelect('select[name="deltaTUnits"]'),
      unitsAbbr:
        readOptionalSelect('select[name="unitsAbbr"]') ??
        readOptionalText('input[name="unitsAbbr"]'),
      ifMissing: readOptionalSelect('select[name="ifMissing"]'),
      roleInput,
    };
  };

  const mergeString = (next: string | undefined, current: string | undefined) => {
    if (next === undefined) return current;
    return next.length > 0 ? next : undefined;
  };

  const buildParm = (values: RowValues, base: CompParm = {}): CompParm => {
    const parsedDeltaT =
      values.deltaT === undefined ? undefined : Number.parseInt(values.deltaT, 10);

    return {
      ...base,
      algoRoleName: values.algoRoleName,
      algoParmType: values.algoParmType,
      siteName: mergeString(values.siteName, base.siteName),
      dataType: mergeString(values.dataType, base.dataType),
      interval: mergeString(values.interval, base.interval),
      paramType: mergeString(values.paramType, base.paramType),
      duration: mergeString(values.duration, base.duration),
      version: mergeString(values.version, base.version),
      deltaT:
        parsedDeltaT === undefined || Number.isNaN(parsedDeltaT)
          ? (base.deltaT ?? 0)
          : parsedDeltaT,
      deltaTUnits: mergeString(values.deltaTUnits, base.deltaTUnits),
      unitsAbbr: mergeString(values.unitsAbbr, base.unitsAbbr),
      ifMissing: mergeString(values.ifMissing, base.ifMissing),
    };
  };

  const renderTextInput = useCallback(
    (name: string, defaultValue: string, ariaLabel: string) =>
      renderToString(
        <Form.Control
          type="text"
          name={name}
          defaultValue={defaultValue}
          size="sm"
          aria-label={ariaLabel}
        />,
      ),
    [],
  );

  const renderCombobox = useCallback(
    (name: string, defaultValue: string, ariaLabel: string, options?: string[]) => {
      const listId = `${name}-options`;
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
            <option value="<var>" />
            {(options ?? []).map((opt) => (
              <option key={opt} value={opt} />
            ))}
          </datalist>
        </>,
      );
    },
    [],
  );

  const parmTypeSelectHtml = useCallback(
    (defaultValue: string, ariaLabel: string) =>
      renderToString(
        <Form.Select
          name="algoParmType"
          defaultValue={defaultValue}
          size="sm"
          aria-label={ariaLabel}
        >
          {PARM_TYPES.map((pt) => (
            <option key={pt.value} value={pt.value}>
              {pt.label}
            </option>
          ))}
        </Form.Select>,
      ),
    [],
  );

  const intervalComboboxHtml = useCallback(
    (defaultValue: string, ariaLabel: string) =>
      renderCombobox("interval", defaultValue, ariaLabel, intervalOptions),
    [intervalOptions, renderCombobox],
  );

  const renderRoleName = useCallback(
    (data: RowParm, type: string) => {
      const parm = data.parm;
      if (type !== "display") return parm.algoRoleName ?? "";
      if (isEditing(data)) {
        return renderTextInput(
          "algoRoleName",
          parm.algoRoleName ?? "",
          t("computations:parms.roleName_input"),
        );
      }
      return parm.algoRoleName ?? "";
    },
    [isEditing, renderTextInput, t],
  );

  const renderParmType = useCallback(
    (data: RowParm, type: string) => {
      const parm = data.parm;
      if (type !== "display") return parmTypeLabel(parm.algoParmType ?? "");
      if (isEditing(data)) {
        return parmTypeSelectHtml(
          parm.algoParmType ?? "i",
          t("computations:parms.parmType_input"),
        );
      }
      return parmTypeLabel(parm.algoParmType ?? "");
    },
    [isEditing, parmTypeSelectHtml, t],
  );

  const renderSiteName = useCallback(
    (data: RowParm, type: string) => {
      const parm = data.parm;
      if (type !== "display") return parm.siteName ?? "";
      if (isEditing(data)) {
        return renderCombobox(
          "siteName",
          parm.siteName ?? "",
          t("computations:parms.site_input"),
        );
      }
      return parm.siteName ?? "";
    },
    [isEditing, renderCombobox, t],
  );

  const renderDataType = useCallback(
    (data: RowParm, type: string) => {
      const parm = data.parm;
      if (type !== "display") return parm.dataType ?? "";
      if (isEditing(data)) {
        return renderCombobox(
          "dataType",
          parm.dataType ?? "",
          t("computations:parms.dataType_input"),
        );
      }
      return parm.dataType ?? "";
    },
    [isEditing, renderCombobox, t],
  );

  const renderInterval = useCallback(
    (data: RowParm, type: string) => {
      const parm = data.parm;
      if (type !== "display") return parm.interval ?? "";
      if (isEditing(data)) {
        return intervalComboboxHtml(
          parm.interval ?? "",
          t("computations:parms.interval_input"),
        );
      }
      return parm.interval ?? "";
    },
    [intervalComboboxHtml, isEditing, t],
  );

  const renderParamType = useCallback(
    (data: RowParm, type: string) => {
      const parm = data.parm;
      if (type !== "display") return parm.paramType ?? "";
      if (isEditing(data)) {
        return renderCombobox(
          "paramType",
          parm.paramType ?? "",
          t("computations:parms.paramType_input"),
        );
      }
      return parm.paramType ?? "";
    },
    [isEditing, renderCombobox, t],
  );

  const renderDuration = useCallback(
    (data: RowParm, type: string) => {
      const parm = data.parm;
      if (type !== "display") return parm.duration ?? "";
      if (isEditing(data)) {
        return renderCombobox(
          "duration",
          parm.duration ?? "",
          t("computations:parms.duration_input"),
        );
      }
      return parm.duration ?? "";
    },
    [isEditing, renderCombobox, t],
  );

  const renderVersion = useCallback(
    (data: RowParm, type: string) => {
      const parm = data.parm;
      if (type !== "display") return parm.version ?? "";
      if (isEditing(data)) {
        return renderCombobox(
          "version",
          parm.version ?? "",
          t("computations:parms.version_input"),
        );
      }
      return parm.version ?? "";
    },
    [isEditing, renderCombobox, t],
  );

  const renderDetailContent = useCallback(
    (data: RowParm): string => {
      const parm = data.parm;
      const editing = isEditing(data);

      return renderToString(
        <div className="p-2 border rounded bg-body-tertiary">
          <div className="row g-2">
            <div className="col-md-2">
              <Form.Label className="small mb-1">
                {t("computations:parms.deltaT")}
              </Form.Label>
              {editing ? (
                <Form.Control
                  type="number"
                  size="sm"
                  name="deltaT"
                  defaultValue={parm.deltaT ?? 0}
                  aria-label={t("computations:parms.deltaT_input")}
                />
              ) : (
                <div>{parm.deltaT ?? 0}</div>
              )}
            </div>
            <div className="col-md-3">
              <Form.Label className="small mb-1">
                {t("computations:parms.deltaTUnits")}
              </Form.Label>
              {editing ? (
                <Form.Select
                  size="sm"
                  name="deltaTUnits"
                  defaultValue={parm.deltaTUnits ?? "Seconds"}
                  aria-label={t("computations:parms.deltaTUnits_input")}
                >
                  {DELTA_T_UNITS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </Form.Select>
              ) : (
                <div>{parm.deltaTUnits ?? "-"}</div>
              )}
            </div>
            <div className="col-md-3">
              <Form.Label className="small mb-1">
                {t("computations:parms.units")}
              </Form.Label>
              {editing ? (
                units.ready ? (
                  <UnitsContext value={units}>
                    <UnitSelect
                      size="sm"
                      name="unitsAbbr"
                      current={parm.unitsAbbr ?? ""}
                      aria-label={t("computations:parms.units_input")}
                    />
                  </UnitsContext>
                ) : (
                  <Form.Control
                    type="text"
                    size="sm"
                    name="unitsAbbr"
                    defaultValue={parm.unitsAbbr ?? ""}
                    aria-label={t("computations:parms.units_input")}
                  />
                )
              ) : (
                <div>{parm.unitsAbbr ?? "-"}</div>
              )}
            </div>
            <div className="col-md-4">
              <Form.Label className="small mb-1">
                {t("computations:parms.ifMissing")}
              </Form.Label>
              {editing ? (
                <Form.Select
                  size="sm"
                  name="ifMissing"
                  defaultValue={parm.ifMissing ?? "FAIL"}
                  aria-label={t("computations:parms.ifMissing_input")}
                >
                  {IF_MISSING_ACTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </Form.Select>
              ) : (
                <div>{parm.ifMissing ?? "-"}</div>
              )}
            </div>
          </div>
        </div>,
      );
    },
    [isEditing, t, units],
  );

  const syncDetailRows = useCallback(() => {
    const dt = table.current?.dt();
    if (!dt) return;

    dt.rows().every(function () {
      const data = this.data() as RowParm;
      const key = rowKey(data);
      const expanded = expandedRows.has(key);
      if (expanded) {
        this.child(renderDetailContent(data), "comp-parm-details").show();
      } else {
        this.child(false);
      }
    });
  }, [expandedRows, renderDetailContent, rowKey]);

  const syncIcons = useCallback(() => {
    const dt = table.current?.dt();
    if (!dt) return;
    dt.rows().every(function () {
      const data = this.data() as RowParm;
      const key = rowKey(data);
      const expanded = expandedRows.has(key);
      const icon = (
        this.node() as HTMLTableRowElement | null
      )?.querySelector<HTMLElement>(".details-toggle__icon");
      if (icon) icon.style.transform = expanded ? "rotate(90deg)" : "";
    });
  }, [expandedRows, rowKey]);

  const handleSaveExisting = useCallback(
    (oldRoleName: string) => {
      const node = findRowNode(
        (d) => d.kind === "existing" && (d.parm.algoRoleName ?? "") === oldRoleName,
      );
      if (!node) return;
      const values = readRowValues(node);
      const { algoRoleName, roleInput } = values;
      if (!algoRoleName) {
        roleInput?.classList.add("border-warning");
        return;
      }

      const current = parms.find((p) => (p.algoRoleName ?? "") === oldRoleName) ?? {};
      onUpdate?.(oldRoleName, buildParm(values, current));
      setEditingNames((prev) => {
        const s = new Set(prev);
        s.delete(oldRoleName);
        return s;
      });
    },
    [buildParm, findRowNode, onUpdate, parms],
  );

  const handleSaveNew = useCallback(
    (idx: number) => {
      const node = findRowNode((d) => d.kind === "new" && d.idx === idx);
      if (!node) return;
      const values = readRowValues(node);
      const { algoRoleName, roleInput } = values;
      if (!algoRoleName) {
        roleInput?.classList.add("border-warning");
        return;
      }
      onAdd?.(buildParm(values));
      setNewParms((prev) => prev.filter((p) => p.idx !== idx));
    },
    [buildParm, findRowNode, onAdd],
  );

  const renderDetailsButton = useCallback(
    (data: RowParm) => {
      const key = rowKey(data);
      const roleName = data.parm.algoRoleName ?? "";
      const label = t("computations:parms.show_details_for", { name: roleName });

      return (
        <Button
          variant="outline-secondary"
          size="sm"
          className="details-toggle"
          aria-label={label}
          onClick={(e) => {
            e.stopPropagation();
            const svg = (e.currentTarget as HTMLElement).querySelector<HTMLElement>(
              "svg",
            );
            setExpandedRows((prev) => {
              const next = new Set(prev);
              if (next.has(key)) {
                next.delete(key);
                if (svg) svg.style.transform = "";
              } else {
                next.add(key);
                if (svg) svg.style.transform = "rotate(90deg)";
              }
              return next;
            });
          }}
        >
          <ChevronRight className="details-toggle__icon" />
        </Button>
      );
    },
    [rowKey, t],
  );

  const renderActions = useCallback(
    (data: RowParm) => {
      if (!edit) return <></>;

      if (data.kind === "new") {
        return (
          <>
            <Button
              variant="primary"
              size="sm"
              aria-label={t("computations:parms.save_parm")}
              onClick={() => handleSaveNew(data.idx)}
            >
              <Save />
            </Button>
            <Button
              variant="secondary"
              size="sm"
              aria-label={t("computations:parms.cancel_parm")}
              onClick={() =>
                setNewParms((prev) => prev.filter((p) => p.idx !== data.idx))
              }
            >
              <X />
            </Button>
          </>
        );
      }

      const roleName = data.parm.algoRoleName ?? "";
      if (data.editing) {
        return (
          <>
            <Button
              variant="primary"
              size="sm"
              aria-label={t("computations:parms.save_parm")}
              onClick={() => handleSaveExisting(roleName)}
            >
              <Save />
            </Button>
            <Button
              variant="secondary"
              size="sm"
              aria-label={t("computations:parms.cancel_parm")}
              onClick={() =>
                setEditingNames((prev) => {
                  const s = new Set(prev);
                  s.delete(roleName);
                  return s;
                })
              }
            >
              <X />
            </Button>
          </>
        );
      }

      return (
        <>
          <Button
            variant="warning"
            size="sm"
            aria-label={t("computations:parms.edit_for", { name: roleName })}
            onClick={() => setEditingNames((prev) => new Set([...prev, roleName]))}
          >
            <Pencil />
          </Button>
          <Button
            variant="danger"
            size="sm"
            aria-label={t("computations:parms.delete_for", { name: roleName })}
            onClick={() => onRemove?.(roleName)}
          >
            <Trash />
          </Button>
        </>
      );
    },
    [edit, t, handleSaveNew, handleSaveExisting, onRemove],
  );

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const columns: any[] = useMemo(() => {
    const cols = [
      { data: null, name: "details", className: "dt-center comp-parm-col-details" },
      { data: null, render: renderRoleName, className: "comp-parm-col-role" },
      { data: null, render: renderParmType, className: "comp-parm-col-type" },
      { data: null, render: renderSiteName },
      { data: null, render: renderDataType },
      { data: null, render: renderParamType },
      { data: null, render: renderInterval },
      { data: null, render: renderDuration },
      { data: null, render: renderVersion },
    ];
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    if (edit) cols.push({ data: null, name: "actions" } as any);
    return cols;
  }, [
    edit,
    renderRoleName,
    renderParmType,
    renderSiteName,
    renderDataType,
    renderParamType,
    renderInterval,
    renderDuration,
    renderVersion,
  ]);

  const slots: DataTableSlots = useMemo(
    () => ({ details: renderDetailsButton, actions: renderActions }),
    [renderDetailsButton, renderActions],
  );

  const options: DataTableProps["options"] = useMemo(
    () => ({
      paging: true,
      pageLength: 10,
      responsive: true,
      language: dtLangs.get(i18n.language),
      layout: {
        top1Start: {
          buttons: edit
            ? [
                {
                  text: "+",
                  action: () => {
                    const idx = nextIdx.current++;
                    setNewParms((prev) => [
                      ...prev,
                      {
                        kind: "new",
                        idx,
                        parm: {
                          algoParmType: "i",
                          deltaT: 0,
                          deltaTUnits: "Seconds",
                          ifMissing: "FAIL",
                        },
                      },
                    ]);
                    setExpandedRows((prev) => new Set([...prev, `new:${idx}`]));
                  },
                  attr: { "aria-label": t("computations:parms.add_parm") },
                },
              ]
            : [],
        },
      },
    }),
    [edit, i18n.language, t],
  );

  useEffect(() => {
    const dt = table.current?.dt();
    if (!dt) return;
    dt.rows().invalidate().draw(false);
    syncDetailRows();
    setTimeout(syncIcons, 0);
  }, [allRows, syncDetailRows, syncIcons]);

  useEffect(() => {
    syncDetailRows();
  }, [expandedRows, syncDetailRows]);

  return (
    <DataTable
      key={i18n.language}
      columns={columns}
      data={allRows}
      options={options}
      slots={slots}
      ref={table}
      className="table table-hover table-striped w-100 border"
    >
      <caption className="caption-title-center">
        {t("computations:parms.title")}
      </caption>
      <thead>
        <tr>
          <th>{t("computations:parms.details")}</th>
          <th>{t("computations:parms.roleName")}</th>
          <th>{t("computations:parms.parmType")}</th>
          <th>{t("computations:parms.site")}</th>
          <th>{t("computations:parms.dataType")}</th>
          <th>{t("computations:parms.paramType")}</th>
          <th>{t("computations:parms.interval")}</th>
          <th>{t("computations:parms.duration")}</th>
          <th>{t("computations:parms.version")}</th>
          {edit && <th>{t("translation:actions")}</th>}
        </tr>
      </thead>
    </DataTable>
  );
};
