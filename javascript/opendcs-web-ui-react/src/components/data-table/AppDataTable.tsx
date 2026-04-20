import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import {
  Suspense,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useTranslation } from "react-i18next";
import { dtLangs } from "../../lang";
import { useContextWrapper } from "../../util/ContextWrapper";
import { useTableProcessing } from "./useTableProcessing";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

// =============================================================================
// Types
// =============================================================================

export type RowMode = "show" | "edit" | "new";

export interface ColumnDef<T> {
  /** Row field to display, or `null` for a virtual/custom column. */
  data: (keyof T & string) | null;
  /** Header cell contents rendered inside the <th>. */
  header: ReactNode;
  defaultContent?: string;
  className?: string;
  name?: string;
  orderable?: boolean;
  searchable?: boolean;
  /**
   * DataTables column `render` — returns the cell content for the given
   * render `type` (`"display"`, `"sort"`, `"filter"`, `"type"`). For custom
   * HTML inputs etc., use `renderToString(<Component />)` to stringify.
   * Passed through to DataTables untouched; see datatables.net column.render.
   */
  render?: (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    data: any,
    type: string,
    row: T,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    meta: any,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ) => any;
  /**
   * Makes this column editable when the row is in `"edit"` or `"new"` mode.
   * Only used when the table has `inlineEdit` enabled.
   */
  edit?: ColumnEdit<T>;
}

export interface RowActionContext<T> {
  row: T;
  /** The `<tr>` element for this row — useful when the action needs to read
   *  values from form controls rendered inside the row via `ColumnDef.render`. */
  rowEl: HTMLTableRowElement;
  api: RowActionApi<T>;
}

export interface RowActionApi<T> {
  /** Set a specific mode on the row (or close it with `undefined`). */
  setMode: (row: T, mode: RowMode | undefined) => void;
  /** Toggle the row between open (default `"show"`) and closed. */
  toggle: (row: T) => void;
  /** Close the row. */
  close: (row: T) => void;
}

export interface RowAction<T> {
  /** Stable identifier. Used as `data-row-action` on the rendered button. */
  key: string;
  /** Bootstrap icon class, e.g. `"bi-pencil"`. */
  icon: string;
  /** Bootstrap button variant (without the `btn-` prefix), e.g. `"warning"`. */
  variant: string;
  /** Accessible label; receives the row data. */
  aria: (row: T) => string;
  /** Return `false` to omit this action for the given row. */
  show?: (row: T) => boolean;
  /** Handler invoked when the button is clicked. */
  onClick: (ctx: RowActionContext<T>) => void;
}

export interface DetailActions<TSave> {
  /** Commit the detail: calls `onSave`, closes the detail into `"show"` mode, schedules scroll-to-end. */
  save: (updated: TSave) => void;
  /** Abandon the detail: closes the row and removes from local items if it was a new one. */
  cancel: () => void;
}

export interface HeaderButton {
  /** Button label text (or icon character like `"+"`). */
  text: string;
  /** Accessible label for screen readers. */
  ariaLabel: string;
  onClick: () => void;
}

export interface AddNewConfig<T> {
  /**
   * Build a fresh local row. `nextId` is an auto-generated negative number
   * less than any existing local id, so the new row is unique against
   * server-side positive ids.
   */
  template: (nextId: number) => T;
  ariaLabel: string;
  /** Defaults to `"+"`. */
  icon?: string;
}

export interface ColumnEdit<T> {
  /** HTML string for the cell when its row is in `"edit"` or `"new"` mode. */
  render: (row: T) => string;
  /** Read the edited value back from the cell's form control on save. */
  read: (cell: HTMLElement) => unknown;
}

export interface InlineEditConfig<T> {
  /**
   * Commit an edited existing row. Receives both the original and the merged
   * result of `{...original, ...cellEdits}`. Return `false` to keep the row
   * in edit mode (e.g. validation failure).
   */
  onSave: (original: T, updated: T) => boolean | void;
  /** Commit a new (locally added) row. Return `false` to keep it in new mode. */
  onAdd?: (created: T) => boolean | void;
  /** Delete a row. When present, a delete button shows in `"show"` mode. */
  onRemove?: (row: T) => void;
  /**
   * Build a fresh local new row. When present, a `+` button shows in the
   * table header that adds a local row in `"new"` mode.
   */
  newTemplate?: () => T;
  /** Aria-label generators for the auto-generated action buttons. */
  labels?: {
    edit?: (row: T) => string;
    remove?: (row: T) => string;
    save?: (row: T) => string;
    cancel?: (row: T) => string;
    add?: string;
  };
}

export interface AppDataTableHandle {
  /**
   * Mark the table so that the next time the `data` prop updates (usually
   * after an external refresh) it pages to the last page. Paired with a
   * caller-side `onRefresh()` / refetch. Saves from inside a detail row
   * schedule this automatically.
   */
  scheduleScrollToEnd: () => void;
}

export interface AppDataTableProps<T, TId extends string | number, TSave = T> {
  // --- Data ---
  data: T[];
  loading?: boolean;
  getId: (row: T) => TId;

  // --- Columns (actions column appended automatically if rowActions is given) ---
  columns: ColumnDef<T>[];
  /** Header cell text for the auto-appended actions column. */
  actionsLabel?: string;

  // --- Detail rendering (optional: provide both to enable row expansion) ---
  /**
   * Called to render the child-row body when a row is open. If omitted, row
   * expansion is disabled entirely (no row-click toggle, no child sync).
   */
  renderDetail?: (ctx: {
    row: T;
    mode: RowMode;
    actions: DetailActions<TSave>;
  }) => ReactNode;
  /** Loading placeholder for the Suspense fallback while `renderDetail` resolves. */
  renderSkeleton?: (ctx: { mode: RowMode }) => ReactNode;

  // --- Inline row buttons ---
  rowActions?: RowAction<T>[];

  /**
   * Enables row-level inline editing. Cells with `ColumnDef.edit` swap between
   * read and edit rendering based on each row's mode; the wrapper auto-generates
   * edit/delete/save/cancel buttons and manages local new rows. Mutually
   * exclusive with `renderDetail`.
   */
  inlineEdit?: InlineEditConfig<T>;

  // --- Header area ---
  addNew?: AddNewConfig<T>;
  extraHeaderButtons?: HeaderButton[];

  // --- Callbacks ---
  /**
   * Server-side commit. Called by the wrapper when a detail fires its save
   * action. May return a Promise — if it does, the wrapper awaits it before
   * transitioning the row back to `"show"` so the follow-up detail fetch
   * sees the just-saved data (avoids a save → refetch race).
   */
  onSave?: (updated: TSave) => void | Promise<unknown>;

  // --- Appearance ---
  caption?: ReactNode;
  tableId?: string;
  /** CSS className applied to the rendered `<table>`. */
  tableClassName?: string;

  // --- Escape hatches ---
  /** Merged into the generated DataTable options (user values win for simple keys). */
  dataTableOptions?: Partial<DataTableProps["options"]>;

  // --- Imperative handle (React 19 ref-as-prop) ---
  ref?: React.Ref<AppDataTableHandle>;
}

// =============================================================================
// Internal helpers
// =============================================================================

const ENTITY_ESCAPES: Record<string, string> = {
  "&": "&amp;",
  "<": "&lt;",
  ">": "&gt;",
  '"': "&quot;",
  "'": "&#39;",
};

const escapeHtml = (s: string): string =>
  s.replace(/[&<>"']/g, (c) => ENTITY_ESCAPES[c]);

function renderActionButtonHtml<T>(action: RowAction<T>, row: T): string {
  return (
    `<button type="button" class="btn btn-${action.variant} btn-sm"` +
    ` data-row-action="${escapeHtml(action.key)}"` +
    ` aria-label="${escapeHtml(action.aria(row))}">` +
    `<i class="bi ${escapeHtml(action.icon)}"></i>` +
    `</button>`
  );
}

/**
 * Read edited cell values from a row's DOM and merge with the original row.
 * Only cells for columns with `edit.read` defined contribute values.
 */
function readEditedRow<T>(
  original: T,
  rowEl: HTMLTableRowElement,
  columns: ColumnDef<T>[],
): T {
  const cells = rowEl.querySelectorAll<HTMLTableCellElement>("td");
  const updates: Record<string, unknown> = {};
  columns.forEach((col, idx) => {
    if (!col.edit || !col.data) return;
    const cell = cells[idx];
    if (cell) updates[col.data as string] = col.edit.read(cell);
  });
  return { ...(original as object), ...updates } as T;
}

// Minimum shape of DataTables' per-row API that we rely on inside drawCallback.
type DtRowApi = {
  node: () => Node;
  data: () => unknown;
  child: {
    (node?: Node | false, className?: string): { show?: () => void };
    isShown: () => boolean;
    hide: () => void;
  };
};

// =============================================================================
// Component
// =============================================================================

const BASE_TABLE_CLASS = "table table-hover table-striped w-100 border";
// Applied only when row-click expand is enabled — signals clickability.
const CLICKABLE_ROW_CLASS = "tablerow-cursor";

export function AppDataTable<T, TId extends string | number, TSave = T>(
  props: AppDataTableProps<T, TId, TSave>,
): React.ReactElement {
  const {
    data,
    loading = false,
    getId,
    columns,
    actionsLabel = "Actions",
    renderDetail,
    renderSkeleton,
    rowActions,
    inlineEdit,
    addNew,
    extraHeaderButtons,
    onSave,
    caption,
    tableId,
    tableClassName,
    dataTableOptions,
    ref,
  } = props;

  if (process.env.NODE_ENV !== "production" && inlineEdit && renderDetail) {
    console.warn(
      "AppDataTable: `inlineEdit` and `renderDetail` are mutually exclusive; row expansion will take precedence.",
    );
  }

  const { toDom } = useContextWrapper();
  const table = useRef<DataTableRef>(null);
  const [, i18n] = useTranslation();

  // --- State ----------------------------------------------------------------
  const [localItems, setLocalItems] = useState<T[]>([]);
  // rowState is keyed by stringified row id so both consumer ids (TId) and
  // wrapper-generated synthetic ids (for inline-edit new rows) coexist.
  const [rowState, setRowState] = useState<Record<string, RowMode>>({});

  // Live ref mirrors so drawCallback / click handlers read the latest values.
  const rowStateRef = useRef(rowState);
  const localItemsRef = useRef(localItems);
  useEffect(() => {
    rowStateRef.current = rowState;
  }, [rowState]);
  useEffect(() => {
    localItemsRef.current = localItems;
  }, [localItems]);

  // Child-row caches so in-progress edits survive redraws.
  const childModeRef = useRef<Record<string, RowMode>>({});
  const childNodesRef = useRef<Record<string, Node>>({});

  // Synthetic ids for inline-edit new rows (row object → synthetic string).
  // WeakMap so GC can reclaim rows that leave `localItems`.
  const newRowIdsRef = useRef<WeakMap<object, string>>(new WeakMap());
  const newRowCounterRef = useRef(0);

  // Stringify a row's id, preferring a synthetic id if the row is a pending
  // inline-edit new row. Used for every rowState / cache lookup internally.
  const idOf = useCallback(
    (row: T): string => {
      const synthetic =
        typeof row === "object" && row !== null
          ? newRowIdsRef.current.get(row as object)
          : undefined;
      return synthetic ?? String(getId(row));
    },
    [getId],
  );

  // Per-row data lookup for click handlers, keyed on the <tr> element.
  const rowDataRef = useRef<WeakMap<HTMLTableRowElement, T>>(new WeakMap());

  // Scroll-to-end scheduling (after save / import).
  const pendingNavRef = useRef(false);

  // --- Expose imperative handle --------------------------------------------
  useImperativeHandle(
    ref,
    () => ({
      scheduleScrollToEnd: () => {
        pendingNavRef.current = true;
      },
    }),
    [],
  );

  // --- Derived data ---------------------------------------------------------
  const tableData = useMemo(() => [...data, ...localItems], [data, localItems]);

  // Expansion is enabled only when both detail + skeleton renderers are provided.
  const hasDetail = Boolean(renderDetail && renderSkeleton);

  // --- State mutators (internal; all keys are the stringified row id) ------
  // Defined early so row-action builders below can reference them.
  const setModeByIdStr = useCallback((id: string, mode: RowMode | undefined) => {
    setRowState((prev) => {
      const next = { ...prev };
      if (mode === undefined) delete next[id];
      else next[id] = mode;
      return next;
    });
  }, []);

  const toggleByIdStr = useCallback((id: string) => {
    setRowState((prev) => {
      const next = { ...prev };
      if (next[id] !== undefined) delete next[id];
      else next[id] = "show";
      return next;
    });
  }, []);

  // --- Effective row actions list ------------------------------------------
  // When `inlineEdit` is set, auto-generate edit/delete/save/cancel buttons.
  // `rowActions` (if also provided) are appended as extras.
  const rowActionsList = useMemo<RowAction<T>[]>(() => {
    const extras = rowActions ?? [];
    if (!inlineEdit) return extras;
    const labels = inlineEdit.labels ?? {};
    const inShowMode = (row: T) => rowStateRef.current[idOf(row)] === undefined;
    const inEditMode = (row: T) => {
      const m = rowStateRef.current[idOf(row)];
      return m === "edit" || m === "new";
    };
    const commitSave = (row: T, rowEl: HTMLTableRowElement) => {
      const updated = readEditedRow(row, rowEl, columns);
      const id = idOf(row);
      const isNew = rowStateRef.current[id] === "new";
      if (isNew) {
        if (inlineEdit.onAdd?.(updated) !== false) {
          newRowIdsRef.current.delete(row as object);
          setLocalItems((prev) => prev.filter((r) => r !== row));
          setModeByIdStr(id, undefined);
        }
      } else {
        if (inlineEdit.onSave(row, updated) !== false) {
          setModeByIdStr(id, undefined);
        }
      }
    };
    const commitCancel = (row: T) => {
      const id = idOf(row);
      const isNew = rowStateRef.current[id] === "new";
      if (isNew) {
        newRowIdsRef.current.delete(row as object);
        setLocalItems((prev) => prev.filter((r) => r !== row));
      }
      setModeByIdStr(id, undefined);
    };
    const built: RowAction<T>[] = [
      {
        key: "inline-edit",
        icon: "bi-pencil",
        variant: "warning",
        show: inShowMode,
        aria: (row) => labels.edit?.(row) ?? "Edit",
        onClick: ({ row }) => setModeByIdStr(idOf(row), "edit"),
      },
      ...(inlineEdit.onRemove
        ? [
            {
              key: "inline-delete",
              icon: "bi-trash",
              variant: "danger",
              show: inShowMode,
              aria: (row: T) => labels.remove?.(row) ?? "Delete",
              onClick: ({ row }: RowActionContext<T>) => inlineEdit.onRemove!(row),
            } as RowAction<T>,
          ]
        : []),
      {
        key: "inline-save",
        icon: "bi-check-lg",
        variant: "primary",
        show: inEditMode,
        aria: (row) => labels.save?.(row) ?? "Save",
        onClick: ({ row, rowEl }) => commitSave(row, rowEl),
      },
      {
        key: "inline-cancel",
        icon: "bi-x-lg",
        variant: "secondary",
        show: inEditMode,
        aria: (row) => labels.cancel?.(row) ?? "Cancel",
        onClick: ({ row }) => commitCancel(row),
      },
    ];
    return [...built, ...extras];
  }, [inlineEdit, rowActions, columns, idOf, setModeByIdStr]);
  const hasActionsCol = rowActionsList.length > 0;

  const rowActionApi = useMemo<RowActionApi<T>>(
    () => ({
      setMode: (row, mode) => setModeByIdStr(idOf(row), mode),
      toggle: (row) => toggleByIdStr(idOf(row)),
      close: (row) => setModeByIdStr(idOf(row), undefined),
    }),
    [idOf, setModeByIdStr, toggleByIdStr],
  );

  // --- Build the detail DOM node (called by drawCallback) -------------------
  // Always defined; becomes a no-op when expansion is disabled (it's never
  // called in that case because `hasDetail` gates child-row sync).
  const buildDetailNode = useCallback(
    (row: T, mode: RowMode): Node => {
      if (!renderDetail || !renderSkeleton) return document.createElement("div");
      const id = idOf(row);
      const actions: DetailActions<TSave> = {
        save: async (updated) => {
          // Await the consumer's save (if it's a Promise) before transitioning
          // so the follow-up `renderDetail` fetch reads committed data.
          const result = onSave?.(updated);
          if (result && typeof (result as Promise<unknown>).then === "function") {
            try {
              await result;
            } catch {
              // Caller reported the error; don't swallow here, but also
              // don't block the UI transition — close the row regardless.
            }
          }
          pendingNavRef.current = true;
          setLocalItems((prev) => prev.filter((r) => idOf(r) !== id));
          setModeByIdStr(id, "show");
        },
        cancel: () => {
          const local = localItemsRef.current.find((r) => idOf(r) === id);
          if (local) {
            setLocalItems((prev) => prev.filter((r) => idOf(r) !== id));
          }
          setModeByIdStr(id, undefined);
        },
      };
      return toDom(
        <Suspense fallback={renderSkeleton({ mode })}>
          {renderDetail({ row, mode, actions })}
        </Suspense>,
      );
    },
    [idOf, onSave, setModeByIdStr, toDom, renderDetail, renderSkeleton],
  );

  // Stable ref so the drawCallback closure always sees the latest renderer.
  const buildDetailNodeRef = useRef(buildDetailNode);
  useEffect(() => {
    buildDetailNodeRef.current = buildDetailNode;
  }, [buildDetailNode]);

  const hasInlineEdit = Boolean(inlineEdit);

  // --- DataTable column definitions (+ optional Actions column) ------------
  // When inlineEdit is enabled, wrap each column's render to swap to the
  // `edit.render` output when the row is in "edit" / "new" mode.
  const dtColumns = useMemo(() => {
    const cols = columns.map((c) => {
      const baseRender = c.render;
      const editRender = c.edit?.render;
      const needsWrapper = Boolean(baseRender || (hasInlineEdit && editRender));
      const wrappedRender = needsWrapper
        ? // eslint-disable-next-line @typescript-eslint/no-explicit-any
          (data: any, type: string, row: T, meta: any) => {
            if (type !== "display") {
              return baseRender ? baseRender(data, type, row, meta) : (data ?? "");
            }
            if (hasInlineEdit && editRender) {
              const mode = rowStateRef.current[idOf(row)];
              if (mode === "edit" || mode === "new") return editRender(row);
            }
            return baseRender ? baseRender(data, type, row, meta) : (data ?? "");
          }
        : undefined;
      return {
        data: c.data,
        defaultContent: c.defaultContent ?? "",
        className: c.className,
        name: c.name,
        orderable: c.orderable,
        searchable: c.searchable,
        render: wrappedRender,
      };
    });
    if (hasActionsCol) {
      cols.push({
        data: null,
        defaultContent: "",
        className: undefined,
        name: "actions",
        orderable: false,
        searchable: false,
        render: undefined,
      });
    }
    return cols;
  }, [columns, hasActionsCol, hasInlineEdit, idOf]);

  // --- DataTable options ----------------------------------------------------
  const options: DataTableProps["options"] = {
    paging: true,
    responsive: true,
    stateSave: true,
    processing: true,
    language: dtLangs.get(i18n.language),
    ...dataTableOptions,
    createdRow: (_row, _data, dataIndex) => {
      table.current?.dt()?.row(dataIndex).node().classList.add("child-toggle");
    },
    drawCallback: function () {
      const dt = this.api();
      dt.rows({ page: "current", search: "applied" }).every(function () {
        const rowData = this.data() as T | undefined;
        if (!rowData) return;
        const id = idOf(rowData);
        const row = this as unknown as DtRowApi;
        const rowEl = row.node() as HTMLTableRowElement;
        rowDataRef.current.set(rowEl, rowData);

        // Inject action buttons into the actions cell (last cell).
        if (hasActionsCol) {
          const cell = rowEl.querySelector("td:last-child") as HTMLElement | null;
          if (cell) {
            const html = rowActionsList
              .filter((a) => !a.show || a.show(rowData))
              .map((a) => renderActionButtonHtml(a, rowData))
              .join(" ");
            if (cell.innerHTML !== html) {
              cell.style.whiteSpace = "nowrap";
              cell.innerHTML = html;
            }
          }
        }

        // Sync the child row to rowState: show/hide/reattach/rebuild.
        // Skipped entirely for non-expandable tables.
        if (hasDetail) {
          const state = rowStateRef.current[id];
          const isShown = row.child.isShown();
          const prevMode = childModeRef.current[id];
          if (state !== undefined) {
            if (!isShown || prevMode !== state) {
              if (isShown) row.child.hide();
              if (prevMode !== state) {
                const node = buildDetailNodeRef.current(rowData, state);
                childNodesRef.current[id] = node;
                row.child(node, "child-row").show?.();
              } else {
                const cached = childNodesRef.current[id];
                if (cached) {
                  row.child(cached, "child-row").show?.();
                } else {
                  const node = buildDetailNodeRef.current(rowData, state);
                  childNodesRef.current[id] = node;
                  row.child(node, "child-row").show?.();
                }
              }
              childModeRef.current[id] = state;
            }
          } else if (isShown) {
            row.child(false);
            delete childModeRef.current[id];
            delete childNodesRef.current[id];
          }
        }
      });
    },
    layout: {
      ...dataTableOptions?.layout,
      top1Start: [
        ...(addNew
          ? [
              {
                buttons: [
                  {
                    text: addNew.icon ?? "+",
                    action: () => {
                      // Navigate to first page sorted ascending so the new row is visible.
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
                      setLocalItems((prev) => {
                        // nextId = one less than the smallest existing local numeric id.
                        const minId = prev.reduce((m, r) => {
                          const rid = getId(r);
                          return typeof rid === "number" ? Math.min(m, rid) : m;
                        }, 0);
                        const nextId = minId - 1;
                        const newItem = addNew.template(nextId);
                        const newId = String(getId(newItem));
                        setRowState((prevRS) => ({ ...prevRS, [newId]: "new" }));
                        return [...prev, newItem];
                      });
                    },
                    attr: { "aria-label": addNew.ariaLabel },
                  },
                ],
              },
            ]
          : []),
        ...(inlineEdit?.newTemplate
          ? [
              {
                buttons: [
                  {
                    text: "+",
                    action: () => {
                      const newItem = inlineEdit.newTemplate!();
                      const synthId = `__appdt_new_${++newRowCounterRef.current}`;
                      if (typeof newItem === "object" && newItem !== null) {
                        newRowIdsRef.current.set(newItem as object, synthId);
                      }
                      setLocalItems((prev) => [...prev, newItem]);
                      setRowState((prevRS) => ({ ...prevRS, [synthId]: "new" }));
                    },
                    attr: {
                      "aria-label": inlineEdit.labels?.add ?? "Add",
                    },
                  },
                ],
              },
            ]
          : []),
        ...(extraHeaderButtons?.map((btn) => ({
          buttons: [
            {
              text: btn.text,
              action: () => btn.onClick(),
              attr: { "aria-label": btn.ariaLabel },
            },
          ],
        })) ?? []),
      ],
    },
  };

  // --- Click delegation: action buttons vs row-toggle -----------------------
  useEffect(() => {
    const dt$ = table.current?.dt();
    if (!dt$) return;

    dt$.off("click").on("click", "tbody tr.child-toggle", function (e) {
      const target = e.target as Element;
      const tr = target.closest("tr") as HTMLTableRowElement | null;
      if (!tr || tr.classList.contains("child-row")) return;

      // Row action?
      const btn = target.closest("[data-row-action]") as HTMLButtonElement | null;
      if (btn) {
        const key = btn.getAttribute("data-row-action");
        const action = rowActionsList.find((a) => a.key === key);
        if (action) {
          const rowEl = btn.closest("tr") as HTMLTableRowElement | null;
          const rowData = rowEl ? rowDataRef.current.get(rowEl) : undefined;
          if (rowData && rowEl) {
            e.preventDefault();
            e.stopPropagation();
            action.onClick({ row: rowData, rowEl, api: rowActionApi });
            return;
          }
        }
      }

      // Row-click → toggle expand/collapse. Only when expansion is enabled.
      if (!hasDetail) return;
      e.preventDefault();
      e.stopPropagation();
      const row = table.current!.dt()!.row(tr);
      const rowData = row.data() as T | undefined;
      if (!rowData) return;
      toggleByIdStr(idOf(rowData));
    });
  }, [i18n.language, rowActionsList, rowActionApi, idOf, toggleByIdStr, hasDetail]);

  // --- Language change: DataTable remounts (key={i18n.language}), reset state ---
  useEffect(() => {
    setRowState({});
    setLocalItems([]);
    childModeRef.current = {};
    childNodesRef.current = {};
    // WeakMap entries are GC'd with their row objects; no reset needed.
  }, [i18n.language]);

  // --- Trigger a redraw when rowState changes so drawCallback / cell render
  //     picks up the new state. For inline-edit tables, also invalidate the
  //     row cache so the mode-aware `render` is re-run for edited cells.
  useEffect(() => {
    const dt = table.current?.dt();
    if (!dt) return;
    if (hasInlineEdit) {
      (dt as unknown as { rows: () => { invalidate: () => unknown } })
        .rows()
        .invalidate();
    }
    dt.draw(false);
  }, [rowState, hasInlineEdit]);

  // --- Processing overlay while loading -------------------------------------
  useTableProcessing(table, loading);

  // --- Scroll to the last page on next `data` update if scheduled -----------
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
  }, [data]);

  return (
    <DataTable
      key={i18n.language}
      id={tableId}
      columns={dtColumns}
      data={tableData}
      options={options}
      ref={table}
      className={
        tableClassName ??
        (hasDetail ? `${BASE_TABLE_CLASS} ${CLICKABLE_ROW_CLASS}` : BASE_TABLE_CLASS)
      }
    >
      {caption && <caption className="caption-title-center">{caption}</caption>}
      <thead>
        <tr>
          {columns.map((c, i) => (
            <th key={i}>{c.header}</th>
          ))}
          {hasActionsCol && <th>{actionsLabel}</th>}
        </tr>
      </thead>
    </DataTable>
  );
}
