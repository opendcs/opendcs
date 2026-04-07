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
import { PARM_TYPES, parmTypeLabel, type ParmTypeOption } from "../common/parmTypes";
import { queryDataTableRowNode } from "../../../util/DataTables";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(dtButtons);

export type AlgoParm = { roleName: string; parmType: string };
export { PARM_TYPES, parmTypeLabel };
export type { ParmTypeOption };

type ExistingRow = {
  kind: "existing";
  roleName: string;
  parmType: string;
  editing: boolean;
};
type NewRow = { kind: "new"; idx: number; parmType: string };
type RowParm = ExistingRow | NewRow;

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
  const [t, i18n] = useTranslation(["algorithms", "translation"]);
  const table = useRef<DataTableRef>(null);
  const [newParms, setNewParms] = useState<NewRow[]>([]);
  const [editingNames, setEditingNames] = useState<Set<string>>(new Set());
  const nextIdx = useRef(0);

  const allRows = useMemo<RowParm[]>(
    () => [
      ...parms.map(
        (p): ExistingRow => ({
          kind: "existing",
          roleName: p.roleName,
          parmType: p.parmType,
          editing: editingNames.has(p.roleName),
        }),
      ),
      ...newParms,
    ],
    [parms, newParms, editingNames],
  );

  // Locate a row's TR node by matching against row data objects
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
    const roleInput = queryDataTableRowNode<HTMLInputElement>(
      node,
      'input[name="roleName"]',
    );
    const typeSelect = queryDataTableRowNode<HTMLSelectElement>(
      node,
      'select[name="parmType"]',
    );
    return {
      roleName: roleInput?.value.trim() ?? "",
      parmType: typeSelect?.value ?? "i",
      roleInput,
    };
  };

  const parmTypeSelectHtml = useCallback(
    (defaultValue: string) =>
      renderToString(
        <Form.Select name="parmType" defaultValue={defaultValue} size="lg">
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
      if (type !== "display") return data.kind === "existing" ? data.roleName : "";
      const needsInput =
        data.kind === "new" || (data.kind === "existing" && data.editing);
      if (needsInput) {
        return renderToString(
          <Form.Control
            type="text"
            name="roleName"
            defaultValue={data.kind === "existing" ? data.roleName : ""}
            size="lg"
            aria-label={t("algorithms:parms.roleName_input")}
          />,
        );
      }
      return data.roleName;
    },
    [t],
  );

  const renderParmType = useCallback(
    (data: RowParm, type: string) => {
      if (type !== "display") return parmTypeLabel(data.parmType ?? "");
      const needsSelect =
        data.kind === "new" || (data.kind === "existing" && data.editing);
      if (needsSelect) return parmTypeSelectHtml(data.parmType ?? "i");
      return parmTypeLabel(data.parmType ?? "");
    },
    [parmTypeSelectHtml],
  );

  const handleSaveExisting = useCallback(
    (oldRoleName: string) => {
      const node = findRowNode(
        (d) => d.kind === "existing" && d.roleName === oldRoleName,
      );
      if (!node) return;
      const { roleName, parmType, roleInput } = readRowValues(node);
      if (!roleName) {
        roleInput?.classList.add("border-warning");
        return;
      }
      onUpdate?.(oldRoleName, { roleName, parmType });
      setEditingNames((prev) => {
        const s = new Set(prev);
        s.delete(oldRoleName);
        return s;
      });
    },
    [findRowNode, onUpdate],
  );

  const handleSaveNew = useCallback(
    (idx: number) => {
      const node = findRowNode((d) => d.kind === "new" && d.idx === idx);
      if (!node) return;
      const { roleName, parmType, roleInput } = readRowValues(node);
      if (!roleName) {
        roleInput?.classList.add("border-warning");
        return;
      }
      onAdd?.({ roleName, parmType });
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
              aria-label={t("algorithms:parms.save_parm")}
              onClick={() => handleSaveNew(data.idx)}
            >
              <Save />
            </Button>
            <Button
              variant="secondary"
              size="lg"
              aria-label={t("algorithms:parms.cancel_parm")}
              onClick={() =>
                setNewParms((prev) => prev.filter((p) => p.idx !== data.idx))
              }
            >
              <X />
            </Button>
          </>
        );
      }

      if (data.editing) {
        return (
          <>
            <Button
              variant="primary"
              size="lg"
              aria-label={t("algorithms:parms.save_parm")}
              onClick={() => handleSaveExisting(data.roleName)}
            >
              <Save />
            </Button>
            <Button
              variant="secondary"
              size="lg"
              aria-label={t("algorithms:parms.cancel_parm")}
              onClick={() =>
                setEditingNames((prev) => {
                  const s = new Set(prev);
                  s.delete(data.roleName);
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
            aria-label={t("algorithms:parms.edit_for", { name: data.roleName })}
            onClick={() => setEditingNames((prev) => new Set([...prev, data.roleName]))}
          >
            <Pencil />
          </Button>
          <Button
            variant="danger"
            size="lg"
            aria-label={t("algorithms:parms.delete_for", { name: data.roleName })}
            onClick={() => onRemove?.(data.roleName)}
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
                      { kind: "new", idx, parmType: "i" },
                    ]);
                  },
                  attr: { "aria-label": t("algorithms:parms.add_parm") },
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
      <caption className="caption-title-center">{t("algorithms:parms.title")}</caption>
      <thead>
        <tr>
          <th>{t("algorithms:parms.roleName")}</th>
          <th>{t("algorithms:parms.parmType")}</th>
          {edit && <th>{t("translation:actions")}</th>}
        </tr>
      </thead>
    </DataTable>
  );
};
