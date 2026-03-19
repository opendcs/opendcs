import DataTable, {
  type DataTableProps,
  type DataTableRef,
  type DataTableSlots,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5";
import { useTranslation } from "react-i18next";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { dtLangs } from "../../../lang";
import { Button, Form } from "react-bootstrap";
import { Pencil, Save, Trash, X } from "react-bootstrap-icons";
import { renderToString } from "react-dom/server";
import type { ApiCompParm } from "opendcs-api";
import { PARM_TYPES, parmTypeLabel, type ParmTypeOption } from "../common/parmTypes";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(dtButtons);

export type CompParm = ApiCompParm;
export { PARM_TYPES, parmTypeLabel };
export type { ParmTypeOption };

type ExistingRow = {
  kind: "existing";
  parm: CompParm;
  editing: boolean;
};
type NewRow = { kind: "new"; idx: number; algoParmType: string };
type RowParm = ExistingRow | NewRow;

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
  const table = useRef<DataTableRef>(null);
  const [newParms, setNewParms] = useState<NewRow[]>([]);
  const [editingNames, setEditingNames] = useState<Set<string>>(new Set());
  const nextIdx = useRef(0);

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

  const readRowValues = (node: HTMLTableRowElement) => {
    const roleInput = node.querySelector<HTMLInputElement>(
      'input[name="algoRoleName"]',
    );
    const typeSelect = node.querySelector<HTMLSelectElement>(
      'select[name="algoParmType"]',
    );
    return {
      algoRoleName: roleInput?.value.trim() ?? "",
      algoParmType: typeSelect?.value ?? "i",
      roleInput,
    };
  };

  const parmTypeSelectHtml = useCallback(
    (defaultValue: string) =>
      renderToString(
        <Form.Select name="algoParmType" defaultValue={defaultValue} size="lg">
          {PARM_TYPES.map((pt) => (
            <option key={pt.value} value={pt.value}>
              {pt.label}
            </option>
          ))}
        </Form.Select>,
      ),
    [],
  );

  const renderRoleName = useCallback(
    (data: RowParm, type: string) => {
      if (type !== "display") {
        return data.kind === "existing" ? (data.parm.algoRoleName ?? "") : "";
      }
      const needsInput =
        data.kind === "new" || (data.kind === "existing" && data.editing);
      if (needsInput) {
        return renderToString(
          <Form.Control
            type="text"
            name="algoRoleName"
            defaultValue={
              data.kind === "existing" ? (data.parm.algoRoleName ?? "") : ""
            }
            size="lg"
            aria-label={t("computations:parms.roleName_input")}
          />,
        );
      }
      return data.parm.algoRoleName ?? "";
    },
    [t],
  );

  const renderParmType = useCallback(
    (data: RowParm, type: string) => {
      if (type !== "display") {
        return parmTypeLabel(
          data.kind === "existing" ? (data.parm.algoParmType ?? "") : data.algoParmType,
        );
      }
      const needsSelect =
        data.kind === "new" || (data.kind === "existing" && data.editing);
      if (needsSelect) {
        const value =
          data.kind === "existing"
            ? (data.parm.algoParmType ?? "i")
            : data.algoParmType;
        return parmTypeSelectHtml(value);
      }
      return parmTypeLabel(data.parm.algoParmType ?? "");
    },
    [parmTypeSelectHtml],
  );

  const handleSaveExisting = useCallback(
    (oldRoleName: string) => {
      const node = findRowNode(
        (d) => d.kind === "existing" && (d.parm.algoRoleName ?? "") === oldRoleName,
      );
      if (!node) return;
      const { algoRoleName, algoParmType, roleInput } = readRowValues(node);
      if (!algoRoleName) {
        roleInput?.classList.add("border-warning");
        return;
      }

      const current = parms.find((p) => (p.algoRoleName ?? "") === oldRoleName) ?? {};
      onUpdate?.(oldRoleName, {
        ...current,
        algoRoleName,
        algoParmType,
      });
      setEditingNames((prev) => {
        const s = new Set(prev);
        s.delete(oldRoleName);
        return s;
      });
    },
    [findRowNode, onUpdate, parms],
  );

  const handleSaveNew = useCallback(
    (idx: number) => {
      const node = findRowNode((d) => d.kind === "new" && d.idx === idx);
      if (!node) return;
      const { algoRoleName, algoParmType, roleInput } = readRowValues(node);
      if (!algoRoleName) {
        roleInput?.classList.add("border-warning");
        return;
      }
      onAdd?.({ algoRoleName, algoParmType });
      setNewParms((prev) => prev.filter((p) => p.idx !== idx));
    },
    [findRowNode, onAdd],
  );

  const renderActions = useCallback(
    (data: RowParm) => {
      if (!edit) return <></>;

      if (data.kind === "new") {
        return (
          <>
            <Button
              variant="primary"
              size="lg"
              aria-label={t("computations:parms.save_parm")}
              onClick={() => handleSaveNew(data.idx)}
            >
              <Save />
            </Button>
            <Button
              variant="secondary"
              size="lg"
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
              size="lg"
              aria-label={t("computations:parms.save_parm")}
              onClick={() => handleSaveExisting(roleName)}
            >
              <Save />
            </Button>
            <Button
              variant="secondary"
              size="lg"
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
            size="lg"
            aria-label={t("computations:parms.edit_for", { name: roleName })}
            onClick={() => setEditingNames((prev) => new Set([...prev, roleName]))}
          >
            <Pencil />
          </Button>
          <Button
            variant="danger"
            size="lg"
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
      { data: null, render: renderRoleName },
      { data: null, render: renderParmType },
    ];
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    if (edit) cols.push({ data: null, name: "actions" } as any);
    return cols;
  }, [edit, renderRoleName, renderParmType]);

  const slots: DataTableSlots = useMemo(
    () => ({ actions: renderActions }),
    [renderActions],
  );

  const options: DataTableProps["options"] = useMemo(
    () => ({
      paging: false,
      scrollY: "calc(10 * 2rem)",
      scrollCollapse: true,
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
                      { kind: "new", idx, algoParmType: "i" },
                    ]);
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
    table.current?.dt()?.rows().invalidate().draw(false);
  }, [allRows]);

  return (
    <DataTable
      key={i18n.language}
      columns={columns}
      data={allRows}
      options={options}
      slots={edit ? slots : undefined}
      ref={table}
      className="table table-hover table-striped w-100 border"
    >
      <caption className="caption-title-center">
        {t("computations:parms.title")}
      </caption>
      <thead>
        <tr>
          <th>{t("computations:parms.roleName")}</th>
          <th>{t("computations:parms.parmType")}</th>
          {edit && <th>{t("translation:actions")}</th>}
        </tr>
      </thead>
    </DataTable>
  );
};
