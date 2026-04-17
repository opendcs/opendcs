import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import { useTranslation } from "react-i18next";
import { Suspense, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { dtLangs } from "../../../lang";
import type { ApiAlgorithm, ApiAlgorithmRef, ApiPropSpec } from "opendcs-api";
import Algorithm, { AlgorithmSkeleton, type UiAlgorithm } from "./Algorithm";
import { useTableProcessing } from "../../../components/data-table";
import type { AlgoParm } from "./AlgorithmParamsTable";
import { CheckForNewModal } from "./CheckForNewModal";
import type { RemoveAction, SaveAction, UiState } from "../../../util/Actions";
import { useContextWrapper } from "../../../util/ContextWrapper";
import type { RowState } from "../../../util/DataTables";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export type TableAlgorithmRef = Partial<ApiAlgorithmRef>;

export interface AlgorithmsTableProperties {
  algorithms: TableAlgorithmRef[];
  getAlgorithm?: (algorithmId: number) => Promise<ApiAlgorithm>;
  getPropSpecs?: (execClass: string) => Promise<ApiPropSpec[]>;
  actions?: SaveAction<ApiAlgorithm> & RemoveAction<number>;
  onRefresh?: () => void;
  loading?: boolean;
}

export const AlgorithmsTable: React.FC<AlgorithmsTableProperties> = ({
  algorithms,
  getAlgorithm,
  getPropSpecs,
  actions = {},
  onRefresh,
  loading = false,
}) => {
  const { toDom } = useContextWrapper();
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["algorithms", "translation"]);
  const [localAlgorithms, updateLocalAlgorithms] = useState<TableAlgorithmRef[]>([]);
  const [rowState, updateRowState] = useState<RowState<number>>({});
  const rowStateRef = useRef(rowState);
  const localAlgorithmsRef = useRef(localAlgorithms);
  const renderAlgorithmRef = useRef<(data: TableAlgorithmRef, edit: boolean) => Node>(
    () => document.createElement("div"),
  );
  // Track what mode each child row was opened in so we can re-render on mode change
  const childModeRef = useRef<Record<number, UiState>>({});
  // Cache child row DOM nodes so in-progress edits survive data changes
  const childNodesRef = useRef<Record<number, Node>>({});
  // Navigate to last page on next draw (after save/import adds a new algorithm)
  const pendingNavRef = useRef(false);
  const [showCheckNew, setShowCheckNew] = useState(false);

  useEffect(() => {
    rowStateRef.current = rowState;
  }, [rowState]);

  useEffect(() => {
    localAlgorithmsRef.current = localAlgorithms;
  }, [localAlgorithms]);

  const algorithmData = useMemo(
    () => [...algorithms, ...localAlgorithms],
    [algorithms, localAlgorithms],
  );

  const columns = [
    { data: "algorithmId", defaultContent: "new", className: "dt-left" },
    { data: "algorithmName", defaultContent: "" },
    { data: "execClass", defaultContent: "" },
    { data: "numCompsUsing", defaultContent: "", className: "dt-left" },
    { data: "description", defaultContent: "" },
    {
      data: null,
      name: "actions",
      orderable: false,
      searchable: false,
      defaultContent: "",
    },
  ];

  const options: DataTableProps["options"] = {
    paging: true,
    responsive: true,
    stateSave: true,
    processing: true,
    language: dtLangs.get(i18n.language),
    createdRow: (_row, _data, dataIndex) => {
      table.current?.dt()?.row(dataIndex).node().classList.add("child-toggle");
    },
    drawCallback: function () {
      const dt = this.api();
      dt.rows({ page: "current", search: "applied" }).every(function () {
        const data = this.data() as TableAlgorithmRef | undefined;
        if (!data) return;
        const idx = data.algorithmId!;

        // Inject action buttons into the last cell if empty
        const node = this.node() as HTMLTableRowElement;
        const actionsCell = node.querySelector("td:last-child");
        if (actionsCell && actionsCell.querySelector(".dt-action-edit") === null) {
          (actionsCell as HTMLElement).style.whiteSpace = "nowrap";
          const editLabel = t("algorithms:editor.edit_for", { id: idx });
          const deleteLabel = t("algorithms:editor.delete_for", { id: idx });
          const editBtn =
            `<button class="btn btn-warning btn-sm dt-action-edit" data-id="${idx}"` +
            ` aria-label="${editLabel}">` +
            `<i class="bi bi-pencil"></i></button>`;
          const deleteBtn =
            idx > 0
              ? ` <button class="btn btn-danger btn-sm dt-action-delete" data-id="${idx}"` +
                ` aria-label="${deleteLabel}">` +
                `<i class="bi bi-trash"></i></button>`
              : "";
          actionsCell.innerHTML = editBtn + deleteBtn;
        }

        // Sync child rows
        const state = rowStateRef.current[idx];
        const isShown = this.child.isShown();
        const prevMode = childModeRef.current[idx];
        if (state !== undefined) {
          if (!isShown || prevMode !== state) {
            if (isShown) {
              this.child.hide();
            }
            if (prevMode !== state) {
              // Mode changed — create fresh content
              const edit = state !== "show";
              const node = renderAlgorithmRef.current(data, edit);
              childNodesRef.current[idx] = node;
              this.child(node, "child-row").show();
            } else {
              // Same mode, just reattach the cached node
              const cached = childNodesRef.current[idx];
              if (cached) {
                this.child(cached, "child-row").show();
              } else {
                const edit = state !== "show";
                const node = renderAlgorithmRef.current(data, edit);
                childNodesRef.current[idx] = node;
                this.child(node, "child-row").show();
              }
            }
            childModeRef.current[idx] = state;
          }
        } else if (isShown) {
          this.child(false);
          delete childModeRef.current[idx];
          delete childNodesRef.current[idx];
        }
      });
    },
    layout: {
      top1Start: [
        {
          buttons: [
            {
              text: "+",
              action: () => {
                const dt = table.current?.dt();
                if (dt) {
                  const currentPage = dt.page();
                  const currentOrder = dt.order();
                  const needsNav =
                    currentPage !== 0 ||
                    currentOrder.length === 0 ||
                    currentOrder[0][0] !== 0 ||
                    currentOrder[0][1] !== "asc";
                  if (needsNav) {
                    dt.order([0, "asc"]).page("first").draw(false);
                  }
                }
                updateLocalAlgorithms((prev) => {
                  const existing = prev
                    .map((v) => v.algorithmId!)
                    .sort((a, b) => a - b);
                  const newId = existing.length > 0 ? existing[0] - 1 : -1;
                  updateRowState((prevRowState) => ({
                    ...prevRowState,
                    [newId]: "new",
                  }));
                  return [...prev, { algorithmId: newId }];
                });
              },
              attr: { "aria-label": t("algorithms:add_algorithm") },
            },
          ],
        },
        {
          buttons: [
            {
              text: t("algorithms:check_new.button"),
              action: () => {
                setShowCheckNew(true);
              },
              attr: { "aria-label": t("algorithms:check_new.button") },
            },
          ],
        },
      ],
    },
  };

  const renderAlgorithm = useCallback(
    (data: TableAlgorithmRef, edit: boolean = false): Node => {
      const algorithmPromise: Promise<UiAlgorithm> =
        data.algorithmId && data.algorithmId > 0
          ? getAlgorithm!(data.algorithmId!)
          : Promise.resolve({ algorithmId: data.algorithmId! } as UiAlgorithm);

      const parmsPromise: Promise<AlgoParm[]> = algorithmPromise.then(
        (algo) => (algo.parms ?? []) as AlgoParm[],
      );

      const propSpecs: Promise<ApiPropSpec[]> =
        getPropSpecs && data.execClass
          ? getPropSpecs(data.execClass)
          : Promise.resolve([]);

      const node = toDom(
        <Suspense
          fallback={<AlgorithmSkeleton edit={edit} className="child-row-opening" />}
        >
          <Algorithm
            algorithm={algorithmPromise}
            propSpecs={propSpecs}
            initialParms={parmsPromise}
            actions={{
              save: (algo: ApiAlgorithm) => {
                actions.save!(algo);
                pendingNavRef.current = true;
                delete childModeRef.current[algo.algorithmId!];
                delete childNodesRef.current[algo.algorithmId!];
                updateLocalAlgorithms((prev) => [
                  ...prev.filter((pa) => pa.algorithmId !== algo.algorithmId),
                ]);
                updateRowState((prev) => {
                  const { [algo.algorithmId!]: _, ...states } = prev;
                  return { ...states, [algo.algorithmId!]: "show" };
                });
              },
              cancel: (item) => {
                delete childModeRef.current[item];
                delete childNodesRef.current[item];
                const local = localAlgorithmsRef.current.find(
                  (la) => la.algorithmId === item,
                );
                if (local) {
                  updateLocalAlgorithms((prev) => [
                    ...prev.filter((pla) => pla.algorithmId !== item),
                  ]);
                }
                updateRowState((prev) => {
                  const { [item]: _, ...states } = prev;
                  return { ...states };
                });
              },
            }}
            edit={edit}
          />
        </Suspense>,
      );
      return node;
    },
    [getAlgorithm, getPropSpecs, toDom, actions.save],
  );

  useEffect(() => {
    renderAlgorithmRef.current = renderAlgorithm;
  }, [renderAlgorithm]);

  // Navigate to last page after server data refreshes from a save/import.
  // Uses `algorithms` (not `algorithmData`) so it only fires after the
  // refetch completes, not on intermediate local state changes.
  useEffect(() => {
    if (!pendingNavRef.current) return;
    pendingNavRef.current = false;
    requestAnimationFrame(() => {
      const dt = table.current?.dt();
      if (!dt) return;
      const lastPage = dt.page.info().pages - 1;
      if (dt.page() !== lastPage) {
        dt.page(lastPage).draw(false);
      }
    });
  }, [algorithms]);

  // When the language changes, the DataTable remounts (key={i18n.language}).
  // Reset all state so stale refs don't leak across remounts.
  useEffect(() => {
    updateRowState({});
    updateLocalAlgorithms([]);
    childModeRef.current = {};
    childNodesRef.current = {};
  }, [i18n.language]);

  useEffect(() => {
    const dt$ = table.current?.dt();
    if (!dt$) return;

    dt$.off("click").on("click", "tbody tr.child-toggle", function (e) {
      const target = e.target! as Element;
      const tr = target.closest("tr");
      if (tr?.classList.contains("child-row")) return;

      // Handle edit button click
      const editBtn = target.closest(".dt-action-edit");
      if (editBtn) {
        e.preventDefault();
        e.stopPropagation();
        const id = Number(editBtn.getAttribute("data-id"));
        updateRowState((prev) => ({ ...prev, [id]: "edit" }));
        return;
      }

      // Handle delete button click
      const deleteBtn = target.closest(".dt-action-delete");
      if (deleteBtn) {
        e.preventDefault();
        e.stopPropagation();
        const id = Number(deleteBtn.getAttribute("data-id"));
        actions.remove?.(id);
        return;
      }

      // Row click — toggle expand/collapse
      if (getAlgorithm === undefined) return;
      e.preventDefault();
      e.stopPropagation();
      const row = table.current!.dt()!.row(tr as HTMLTableRowElement);
      updateRowState((prev) => {
        const idx = (row.data() as TableAlgorithmRef).algorithmId!;
        const { [idx]: existing, ...remaining } = prev;
        let newValue: UiState = "show";
        if (existing !== undefined) {
          newValue = undefined;
        }
        return { ...remaining, [idx]: newValue };
      });
    });
  }, [i18n.language, getAlgorithm, actions.remove]);

  // Trigger a DataTable redraw when rowState changes so drawCallback
  // can sync child rows. This is safe because drawCallback handles
  // the actual child row logic after DataTable finishes rendering.
  useEffect(() => {
    table.current?.dt()?.draw(false);
  }, [rowState]);

  // Show DataTables' built-in "Processing..." overlay while the algorithm refs
  // fetch is in flight so the empty table doesn't look like "no algorithms".
  useTableProcessing(table, loading);

  return (
    <>
      <DataTable
        key={i18n.language}
        id="algorithmTable"
        columns={columns}
        data={algorithmData}
        options={options}
        ref={table}
        className="table table-hover table-striped tablerow-cursor w-100 border"
      >
        <caption className="caption-title-center">
          {t("algorithms:algorithmsTitle")}
        </caption>
        <thead>
          <tr>
            <th>{t("algorithms:header.Id")}</th>
            <th>{t("algorithms:header.Name")}</th>
            <th>{t("algorithms:header.ExecClass")}</th>
            <th>{t("algorithms:header.NumCompsUsing")}</th>
            <th>{t("algorithms:header.Description")}</th>
            <th>{t("translation:actions")}</th>
          </tr>
        </thead>
      </DataTable>
      <CheckForNewModal
        show={showCheckNew}
        onHide={() => setShowCheckNew(false)}
        onImported={() => {
          pendingNavRef.current = true;
          onRefresh?.();
        }}
      />
    </>
  );
};
