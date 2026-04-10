import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5";
import { useTranslation } from "react-i18next";
import { useCallback, useMemo, useRef, useState } from "react";
import { dtLangs } from "../../../lang";
import { Form } from "react-bootstrap";
import { renderToString } from "react-dom/server";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(dtButtons);

export type AlgoParm = { roleName: string; parmType: string };

/** All parm types from RoleTypes.java */
export const PARM_TYPES: { value: string; label: string }[] = [
  { value: "i", label: "i: Simple Input" },
  { value: "o", label: "o: Simple Output" },
  { value: "id", label: "id: Delta with Implicit Period" },
  { value: "idh", label: "idh: Hourly Delta" },
  { value: "idd", label: "idd: Daily Delta" },
  { value: "idld", label: "idld: Delta from end of last day" },
  { value: "idlm", label: "idlm: Delta from end of last month" },
  { value: "idly", label: "idly: Delta from end of last year" },
  { value: "idlwy", label: "idlwy: Delta from end of last water-year" },
  { value: "id5min", label: "id5min: Delta for last 5 minutes" },
  { value: "id10min", label: "id10min: Delta for last 10 minutes" },
  { value: "id15min", label: "id15min: Delta for last 15 minutes" },
  { value: "id20min", label: "id20min: Delta for last 20 minutes" },
  { value: "id30min", label: "id30min: Delta for last 30 minutes" },
];

export const parmTypeLabel = (type: string): string =>
  PARM_TYPES.find((pt) => pt.value === type)?.label ?? type;

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

  // Refs so drawCallback and event delegation always have latest values
  const onAddRef = useRef(onAdd);
  onAddRef.current = onAdd;
  const onRemoveRef = useRef(onRemove);
  onRemoveRef.current = onRemove;
  const onUpdateRef = useRef(onUpdate);
  onUpdateRef.current = onUpdate;
  const editingNamesRef = useRef(editingNames);
  editingNamesRef.current = editingNames;
  const newParmsRef = useRef(newParms);
  newParmsRef.current = newParms;

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
    const roleInput = node.querySelector<HTMLInputElement>('input[name="roleName"]');
    const typeSelect = node.querySelector<HTMLSelectElement>('select[name="parmType"]');
    return {
      roleName: roleInput?.value.trim() ?? "",
      parmType: typeSelect?.value ?? "i",
      roleInput,
    };
  };

  const parmTypeSelectHtml = useCallback(
    (defaultValue: string) =>
      renderToString(
        <Form.Select name="parmType" defaultValue={defaultValue}>
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
      onUpdateRef.current?.(oldRoleName, { roleName, parmType });
      setEditingNames((prev) => {
        const s = new Set(prev);
        s.delete(oldRoleName);
        return s;
      });
    },
    [findRowNode],
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
      onAddRef.current?.({ roleName, parmType });
      setNewParms((prev) => prev.filter((p) => p.idx !== idx));
    },
    [findRowNode],
  );

  // Refs for event delegation
  const handleSaveExistingRef = useRef(handleSaveExisting);
  handleSaveExistingRef.current = handleSaveExisting;
  const handleSaveNewRef = useRef(handleSaveNew);
  handleSaveNewRef.current = handleSaveNew;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const columns: any[] = useMemo(() => {
    const cols = [
      { data: null, render: renderRoleName },
      { data: null, render: renderParmType },
    ];
    if (edit)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      cols.push({
        data: null,
        name: "actions",
        orderable: false,
        searchable: false,
        defaultContent: "",
      } as any);
    return cols;
  }, [edit, renderRoleName, renderParmType]);

  const options: DataTableProps["options"] = useMemo(
    () => ({
      paging: false,
      scrollY: "calc(10 * 2rem)",
      scrollCollapse: true,
      responsive: true,
      language: dtLangs.get(i18n.language),
      drawCallback: function () {
        if (!edit) return;
        const dt = this.api();
        dt.rows().every(function () {
          const data = this.data() as RowParm | undefined;
          if (!data) return;
          const node = this.node() as HTMLTableRowElement;
          const actionsCell = node.querySelector("td:last-child");
          if (!actionsCell || actionsCell.querySelector(".dt-parm-action") !== null)
            return;
          (actionsCell as HTMLElement).style.whiteSpace = "nowrap";

          if (data.kind === "new") {
            actionsCell.innerHTML =
              `<button class="btn btn-primary btn-sm dt-parm-action dt-parm-save-new" data-idx="${data.idx}"` +
              ` aria-label="${t("algorithms:parms.save_parm")}">` +
              `<i class="bi bi-check-lg"></i></button> ` +
              `<button class="btn btn-secondary btn-sm dt-parm-action dt-parm-cancel-new" data-idx="${data.idx}"` +
              ` aria-label="${t("algorithms:parms.cancel_parm")}">` +
              `<i class="bi bi-x-lg"></i></button>`;
          } else if (data.kind === "existing" && data.editing) {
            actionsCell.innerHTML =
              `<button class="btn btn-primary btn-sm dt-parm-action dt-parm-save-existing" data-role="${data.roleName}"` +
              ` aria-label="${t("algorithms:parms.save_parm")}">` +
              `<i class="bi bi-check-lg"></i></button> ` +
              `<button class="btn btn-secondary btn-sm dt-parm-action dt-parm-cancel-existing" data-role="${data.roleName}"` +
              ` aria-label="${t("algorithms:parms.cancel_parm")}">` +
              `<i class="bi bi-x-lg"></i></button>`;
          } else if (data.kind === "existing") {
            actionsCell.innerHTML =
              `<button class="btn btn-warning btn-sm dt-parm-action dt-parm-edit" data-role="${data.roleName}"` +
              ` aria-label="${t("algorithms:parms.edit_for", { name: data.roleName })}">` +
              `<i class="bi bi-pencil"></i></button> ` +
              `<button class="btn btn-danger btn-sm dt-parm-action dt-parm-delete" data-role="${data.roleName}"` +
              ` aria-label="${t("algorithms:parms.delete_for", { name: data.roleName })}">` +
              `<i class="bi bi-trash"></i></button>`;
          }
        });

        // Event delegation — attach once per draw
        const tableNode = dt.table().node() as HTMLTableElement;
        if (tableNode.dataset.parmDelegated) return;
        tableNode.dataset.parmDelegated = "true";
        tableNode.addEventListener("click", (e) => {
          const target = e.target as Element;

          const saveNew = target.closest(".dt-parm-save-new");
          if (saveNew) {
            e.stopPropagation();
            handleSaveNewRef.current(Number(saveNew.getAttribute("data-idx")));
            return;
          }

          const cancelNew = target.closest(".dt-parm-cancel-new");
          if (cancelNew) {
            e.stopPropagation();
            const idx = Number(cancelNew.getAttribute("data-idx"));
            setNewParms((prev) => prev.filter((p) => p.idx !== idx));
            return;
          }

          const saveExisting = target.closest(".dt-parm-save-existing");
          if (saveExisting) {
            e.stopPropagation();
            handleSaveExistingRef.current(saveExisting.getAttribute("data-role")!);
            return;
          }

          const cancelExisting = target.closest(".dt-parm-cancel-existing");
          if (cancelExisting) {
            e.stopPropagation();
            const role = cancelExisting.getAttribute("data-role")!;
            setEditingNames((prev) => {
              const s = new Set(prev);
              s.delete(role);
              return s;
            });
            return;
          }

          const editBtn = target.closest(".dt-parm-edit");
          if (editBtn) {
            e.stopPropagation();
            const role = editBtn.getAttribute("data-role")!;
            setEditingNames((prev) => new Set([...prev, role]));
            return;
          }

          const deleteBtn = target.closest(".dt-parm-delete");
          if (deleteBtn) {
            e.stopPropagation();
            onRemoveRef.current?.(deleteBtn.getAttribute("data-role")!);
            return;
          }
        });
      },
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

  return (
    <DataTable
      key={i18n.language}
      columns={columns}
      data={allRows}
      options={options}
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
