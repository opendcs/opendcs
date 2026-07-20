import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5";
import { Alert } from "react-bootstrap";
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
import { Button, Modal } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { dtLangs } from "../../lang";
import { useContextWrapper } from "../../util/ContextWrapper";
import { useTableProcessing } from "./useTableProcessing";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);
// Buttons plugin — required because the wrapper uses `layout.top1Start` to
// render `+` / extra header buttons via DataTables' Buttons feature.
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(dtButtons);

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
  /** DataTables column `type` — skip the type auto-sniffer when set. */
  type?: "num" | "string" | "date" | "html" | "html-num" | "num-fmt";
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
  /**
   * When provided, clicking this action shows a confirmation modal with the
   * returned message before invoking `onClick`. The actual `onClick` is only
   * called after the user confirms.
   */
  confirm?: (row: T) => string;
  /** Handler invoked when the button is clicked (after confirmation if `confirm` is set). */
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
  render: (row: T, rowId: string) => string;
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
  onAdd?: (created: T, rowEl: HTMLTableRowElement) => false | "marked" | void;
  /** Delete a row. When present, a delete button shows in `"show"` mode. */
  onRemove?: (row: T) => void;
  /**
   * Build a fresh local new row. When present, a `+` button shows in the
   * table header that adds a local row in `"new"` mode.
   */
  newTemplate?: () => T;
  /** Aria-label generators for the auto-generated action buttons. */
  labels?: {
    edit?: (row: T, rowId: string) => string;
    remove?: (row: T, rowId: string) => string;
    save?: (row: T, rowId: string) => string;
    cancel?: (row: T, rowId: string) => string;
    add?: string;
  };
}

export interface AppDataTableHandle<T = unknown> {
  /**
   * Mark the table so that the next time the `data` prop updates (usually
   * after an external refresh) it pages to the last page. Paired with a
   * caller-side `onRefresh()` / refetch. Saves from inside a detail row
   * schedule this automatically.
   */
  scheduleScrollToEnd: () => void;
  /**
   * Open the row whose id matches `id` in the given `mode` (defaults to
   * `"show"`). If the row exists in the current data set the table is paged
   * to it; otherwise the open is deferred until the row appears via a future
   * `data` update.
   */
  openRow: (id: string | number, mode?: RowMode) => void;
  appendLocalItem: (template: (nextId: number) => T, mode?: RowMode) => void;
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
  ref?: React.Ref<AppDataTableHandle<T>>;
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
  s.replaceAll(/[&<>"']/g, (c) => ENTITY_ESCAPES[c]);

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
    if (cell) updates[col.data] = col.edit.read(cell);
  });
  return { ...original, ...updates } as T;
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

/** Build the DataTables `render` function for a column, accounting for the
 *  mode-aware edit swap. Returns `undefined` if the column needs no custom
 *  render. */
function makeColumnRender<T>(
  col: ColumnDef<T>,
  hasInlineEdit: boolean,
  idOf: (row: T) => string,
  rowStateRef: { readonly current: Record<string, RowMode> },
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
): ((data: any, type: string, row: T, meta: any) => any) | undefined {
  const baseRender = col.render;
  const editRender = col.edit?.render;
  const needsWrapper = Boolean(baseRender || (hasInlineEdit && editRender));
  if (!needsWrapper) return undefined;
  const fallback = (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    data: any,
    type: string,
    row: T,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    meta: any,
  ) => (baseRender ? baseRender(data, type, row, meta) : (data ?? ""));
  return (data, type, row, meta) => {
    if (type !== "display") return fallback(data, type, row, meta);
    if (hasInlineEdit && editRender) {
      const mode = rowStateRef.current[idOf(row)];
      if (mode === "edit" || mode === "new") return editRender(row, idOf(row));
    }
    return fallback(data, type, row, meta);
  };
}

/** Inject the action-button HTML into the last cell of `rowEl`. */
function injectActionCell<T>(
  rowEl: HTMLTableRowElement,
  rowData: T,
  actions: RowAction<T>[],
): void {
  const cell = rowEl.querySelector("td:last-child") as HTMLElement | null;
  if (!cell) return;
  const html = actions
    .filter((a) => !a.show || a.show(rowData))
    .map((a) => renderActionButtonHtml(a, rowData))
    .join(" ");
  if (cell.innerHTML === html) return;
  cell.style.whiteSpace = "nowrap";
  cell.innerHTML = html;
}

interface ChildSyncRefs {
  rowStateRef: { readonly current: Record<string, RowMode> };
  childModeRef: { current: Record<string, RowMode> };
  childNodesRef: { current: Record<string, Node> };
}

/** Show / hide / rebuild / reattach the DataTables child row for one row
 *  based on the current `rowState`. */
function syncChildRow<T>(
  row: DtRowApi,
  rowData: T,
  id: string,
  refs: ChildSyncRefs,
  buildDetailNode: (row: T, mode: RowMode) => Node,
): void {
  const state = refs.rowStateRef.current[id];
  const isShown = row.child.isShown();

  if (state === undefined) {
    if (!isShown) return;
    row.child(false);
    delete refs.childModeRef.current[id];
    delete refs.childNodesRef.current[id];
    return;
  }

  const prevMode = refs.childModeRef.current[id];
  if (isShown && prevMode === state) return;

  if (isShown) row.child.hide();
  const cached = prevMode === state ? refs.childNodesRef.current[id] : undefined;
  const node = cached ?? buildDetailNode(rowData, state);
  refs.childNodesRef.current[id] = node;
  refs.childModeRef.current[id] = state;
  row.child(node, "child-row").show?.();
}

/** Compute the next synthetic negative id for an `addNew` insertion, one
 *  less than the smallest existing local numeric id. */
function nextAddNewId<T>(existing: T[], getId: (row: T) => string | number): number {
  const minId = existing.reduce((m, r) => {
    const rid = getId(r);
    return typeof rid === "number" ? Math.min(m, rid) : m;
  }, 0);
  return minId - 1;
}

/** Filter a local-items array by reference identity — extracted so callers
 *  don't have to stack `setLocalItems((prev) => prev.filter((r) => …))`
 *  (which pushes nesting depth past Sonar's limit). */
function withoutRef<T>(items: T[], target: T): T[] {
  return items.filter((r) => r !== target);
}

/** Same but compares by stringified id via the provided `idOf`. */
function withoutId<T>(items: T[], id: string, idOf: (r: T) => string): T[] {
  return items.filter((r) => idOf(r) !== id);
}

// =============================================================================
// Component
// =============================================================================

const BASE_TABLE_CLASS = "table table-hover table-striped w-100 border";
// Applied only when row-click expand is enabled — signals clickability.
const CLICKABLE_ROW_CLASS = "tablerow-cursor";

export function AppDataTable<T, TId extends string | number, TSave = T>(
  props: Readonly<AppDataTableProps<T, TId, TSave>>,
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
  const [t, i18n] = useTranslation();

  // --- State ----------------------------------------------------------------
  const [localItems, setLocalItems] = useState<T[]>([]);
  // Set when a row's onSave rejects, so the failure is visible instead of
  // silently discarded. Cleared on the next save attempt.
  const [saveError, setSaveError] = useState<string | null>(null);
  // rowState is keyed by stringified row id so both consumer ids (TId) and
  // wrapper-generated synthetic ids (for inline-edit new rows) coexist.
  const [rowState, setRowState] = useState<Record<string, RowMode>>({});
  // Pending confirmation dialog — set when a RowAction with `confirm` is clicked.
  const [pendingConfirm, setPendingConfirm] = useState<{
    message: string;
    onConfirm: () => void;
  } | null>(null);
  // Fade the wrapper in once DataTables finishes its first init. React renders
  // the bare <table> (with <caption> + <thead>) before DataTables inserts the
  // top toolbar (buttons / search / page-length), which would otherwise flash
  // the title at the top and shift it down once the toolbar appears. Uses
  // opacity (not visibility) so an enclosing `DetailFade`'s `visibility: hidden`
  // isn't punched through — a child `visibility: visible` would override the
  // parent, but parent `opacity: 0` always wins over child opacity.
  const [isInitialized, setIsInitialized] = useState(false);

  // Live ref mirrors so drawCallback / click handlers read the latest values.
  // Updated synchronously during render (not in effects) because DataTables'
  // child-before-parent effect ordering means effects run too late — the draw
  // callback fires in the child's effect and would see the previous render's values.
  const rowStateRef = useRef(rowState);
  // eslint-disable-next-line react-hooks/refs
  rowStateRef.current = rowState;
  const localItemsRef = useRef(localItems);
  // eslint-disable-next-line react-hooks/refs
  localItemsRef.current = localItems;

  // Child-row caches so in-progress edits survive redraws.
  const childModeRef = useRef<Record<string, RowMode>>({});
  const childNodesRef = useRef<Record<string, Node>>({});

  // Synthetic ids for inline-edit new rows (row object → synthetic string).
  // WeakMap so GC can reclaim rows that leave `localItems`.
  const newRowIdsRef = useRef<WeakMap<object, string>>(new WeakMap());
  const newRowCounterRef = useRef(0);

  const nextNumericIdRef = useRef<number | null>(null);
  const nextNumericId = useCallback(
    (existing: T[]): number => {
      nextNumericIdRef.current =
        nextNumericIdRef.current === null
          ? nextAddNewId(existing, getId)
          : nextNumericIdRef.current - 1;
      return nextNumericIdRef.current;
    },
    [getId],
  );

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
      if (next[id] === undefined) next[id] = "show";
      else delete next[id];
      return next;
    });
  }, []);

  // Page the table to the row whose id matches `idStr`, when possible. Safe
  // to call when the row isn't loaded yet (just no-ops); the row-state change
  // alone is enough — once the row appears in a future `data` update, the
  // drawCallback / syncChildRow flow opens it.
  const pageToRowId = useCallback(
    (idStr: string) => {
      const dt = table.current?.dt();
      if (!dt) return;
      const ordered = dt
        .rows({ search: "applied", order: "applied" })
        .indexes()
        .toArray();
      const pos = ordered.findIndex(
        (idx: number) => String(getId(dt.row(idx).data() as T)) === idStr,
      );
      if (pos < 0) return;
      const pageLen = dt.page.info().length;
      if (pageLen <= 0) return;
      const targetPage = Math.floor(pos / pageLen);
      if (dt.page() !== targetPage) dt.page(targetPage).draw(false);
    },
    [getId],
  );

  const appendLocalItem = useCallback(
    (template: (nextId: number) => T, mode: RowMode = "new") => {
      setLocalItems((prev) => {
        const newId = nextNumericId(prev);
        const newItem = template(newId);
        const newIdStr = String(getId(newItem));
        setRowState((prevRS) => ({ ...prevRS, [newIdStr]: mode }));
        return [...prev, newItem];
      });
    },
    [getId, nextNumericId],
  );

  // --- Expose imperative handle --------------------------------------------
  useImperativeHandle(
    ref,
    () => ({
      scheduleScrollToEnd: () => {
        pendingNavRef.current = true;
      },
      openRow: (id, mode = "show") => {
        const idStr = String(id);
        setModeByIdStr(idStr, mode);
        pageToRowId(idStr);
      },
      appendLocalItem,
    }),
    [setModeByIdStr, pageToRowId, appendLocalItem],
  );

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
    const dropLocalNew = (row: T, id: string) => {
      newRowIdsRef.current.delete(row as object);
      setLocalItems((prev) => withoutRef(prev, row));
      setModeByIdStr(id, undefined);
    };
    const markInvalidInputs = (rowEl: HTMLTableRowElement) => {
      rowEl
        .querySelectorAll<HTMLInputElement>("input, textarea, select")
        .forEach((el) => {
          if (el.value.trim()) el.classList.remove("border-warning");
          else el.classList.add("border-warning");
        });
    };
    const commitSave = (row: T, rowEl: HTMLTableRowElement) => {
      const updated = readEditedRow(row, rowEl, columns);
      const id = idOf(row);
      const isNew = rowStateRef.current[id] === "new";
      if (isNew) {
        const addResult = inlineEdit.onAdd?.(updated, rowEl);
        if (addResult === false) {
          markInvalidInputs(rowEl);
          return;
        }
        if (addResult === "marked") {
          return;
        }
        dropLocalNew(row, id);
        return;
      }
      if (inlineEdit.onSave(row, updated) === false) {
        markInvalidInputs(rowEl);
        return;
      }
      setModeByIdStr(id, undefined);
    };
    const commitCancel = (row: T) => {
      const id = idOf(row);
      if (rowStateRef.current[id] === "new") {
        dropLocalNew(row, id);
        return;
      }
      setModeByIdStr(id, undefined);
    };
    const built: RowAction<T>[] = [
      {
        key: "inline-edit",
        icon: "bi-pencil",
        variant: "warning",
        show: inShowMode,
        aria: (row) => labels.edit?.(row, idOf(row)) ?? "Edit",
        onClick: ({ row }) => setModeByIdStr(idOf(row), "edit"),
      },
      ...(inlineEdit.onRemove
        ? [
            {
              key: "inline-delete",
              icon: "bi-trash",
              variant: "danger",
              show: inShowMode,
              aria: (row: T) => labels.remove?.(row, idOf(row)) ?? "Delete",
              onClick: ({ row }: RowActionContext<T>) => inlineEdit.onRemove!(row),
            } as RowAction<T>,
          ]
        : []),
      {
        key: "inline-save",
        icon: "bi-check-lg",
        variant: "primary",
        show: inEditMode,
        aria: (row) => labels.save?.(row, idOf(row)) ?? "Save",
        onClick: ({ row, rowEl }) => commitSave(row, rowEl),
      },
      {
        key: "inline-cancel",
        icon: "bi-x-lg",
        variant: "secondary",
        show: inEditMode,
        aria: (row) => labels.cancel?.(row, idOf(row)) ?? "Cancel",
        onClick: ({ row }) => commitCancel(row),
      },
    ];
    return [...built, ...extras];
  }, [inlineEdit, rowActions, columns, idOf, setModeByIdStr]);
  // rowActionsList's `show`/`onClick` closures read rowStateRef.current only
  // when invoked later from DOM event handlers / drawCallback, never
  // synchronously during this render; `.length` here only counts entries.
  // eslint-disable-next-line react-hooks/refs
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
          // Only scroll to the last page when committing a row that was added
          // locally via `addNew` — those land at the end of the table after
          // the server refresh. Edits to existing rows shouldn't move the user.
          const isNewLocal = localItemsRef.current.some((r) => idOf(r) === id);
          // Await the consumer's save (if it's a Promise) before transitioning
          // so the follow-up `renderDetail` fetch reads committed data.
          // `await` works on thenables and passes through non-promises unchanged.
          setSaveError(null);
          try {
            await onSave?.(updated);
          } catch (err) {
            console.error("Failed to save row", err);
            // For new local items, re-throw so the renderDetail component can
            // display the error and the row stays in edit mode (preserving user
            // input). For existing server rows, closing is safe — they remain
            // in the data list with their last-saved values.
            if (isNewLocal) throw err;
            setSaveError(t("save_failed"));
          }
          if (isNewLocal) pendingNavRef.current = true;
          setLocalItems((prev) => withoutId(prev, id, idOf));
          setModeByIdStr(id, "show");
        },
        cancel: () => {
          if (localItemsRef.current.some((r) => idOf(r) === id)) {
            setLocalItems((prev) => withoutId(prev, id, idOf));
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
    [idOf, onSave, setModeByIdStr, toDom, renderDetail, renderSkeleton, t],
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
    // makeColumnRender stores rowStateRef in the returned render closure,
    // which DataTables invokes later during its own cell rendering, not
    // synchronously here.
    // eslint-disable-next-line react-hooks/refs
    const cols = columns.map((c) => ({
      data: c.data,
      defaultContent: c.defaultContent ?? "",
      className: c.className,
      name: c.name,
      orderable: c.orderable,
      searchable: c.searchable,
      type: c.type,
      render: makeColumnRender(c, hasInlineEdit, idOf, rowStateRef),
    }));
    if (hasActionsCol) {
      cols.push({
        data: null,
        defaultContent: "",
        className: "all",
        name: "actions",
        orderable: false,
        searchable: false,
        type: undefined,
        render: undefined,
      });
    }
    return cols;
  }, [columns, hasActionsCol, hasInlineEdit, idOf]);

  // --- DataTable options ----------------------------------------------------
  const options: DataTableProps["options"] = {
    paging: true,
    responsive: !hasInlineEdit,
    stateSave: true,
    processing: true,
    deferRender: true,
    language: dtLangs.get(i18n.language),
    ...dataTableOptions,
    createdRow: (_row, _data, dataIndex) => {
      table.current?.dt()?.row(dataIndex).node().classList.add("child-toggle");
    },
    initComplete: () => {
      setIsInitialized(true);
    },
    drawCallback: function () {
      // DataTables binds `this` to the Api on each callback — read it once
      // and hand everything else off to plain helpers so this component body
      // stays free of `this`-using flows.
      const dt = this.api(); // NOSONAR — DataTables binds `this` to the Api instance in drawCallback
      const childRefs: ChildSyncRefs = {
        rowStateRef,
        childModeRef,
        childNodesRef,
      };
      dt.rows({ page: "current", search: "applied" }).every(function () {
        const rowApi = this as unknown as DtRowApi;
        const rowData = rowApi.data() as T | undefined;
        if (!rowData) return;
        const rowEl = rowApi.node() as HTMLTableRowElement;
        rowDataRef.current.set(rowEl, rowData);
        if (hasActionsCol) injectActionCell(rowEl, rowData, rowActionsList);
        if (hasDetail) {
          syncChildRow(
            rowApi,
            rowData,
            idOf(rowData),
            childRefs,
            buildDetailNodeRef.current,
          );
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
                        const newItem = addNew.template(nextNumericId(prev));
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
      const btn = target.closest<HTMLButtonElement>("[data-row-action]");
      if (btn) {
        const key = btn.dataset.rowAction;
        const action = rowActionsList.find((a) => a.key === key);
        const rowEl = action ? btn.closest<HTMLTableRowElement>("tr") : null;
        const rowData = rowEl ? rowDataRef.current.get(rowEl) : undefined;
        if (action && rowEl && rowData) {
          e.preventDefault();
          e.stopPropagation();
          if (action.confirm) {
            setPendingConfirm({
              message: action.confirm(rowData),
              onConfirm: () => {
                setPendingConfirm(null);
                action.onClick({ row: rowData, rowEl, api: rowActionApi });
              },
            });
          } else {
            action.onClick({ row: rowData, rowEl, api: rowActionApi });
          }
          return;
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

  // --- Language change: DataTable remounts (key=tableKey), reset state ---
  // Only remount and reset on a *user-driven* language switch, not on the
  // initial settling from i18next's language detector. Resetting during
  // startup wipes any state a parent set on mount (e.g. SitesPage's
  // deep-link openRow) before the first draw, so rows opened by the parent
  // never appear. Using a separate `tableKey` (instead of `i18n.language`
  // directly) means the DataTable key stays stable during the initial
  // detect → settle transition (e.g. "en" → "en-US"), preventing mid-test
  // remounts that close open detail rows.
  const [tableKey, setTableKey] = useState(() => i18n.language ?? "__initial__");
  const langInitRef = useRef<string | null>(null);
  useEffect(() => {
    if (!i18n.isInitialized) return;
    if (langInitRef.current === null) {
      langInitRef.current = i18n.language;
      return; // initial settle: keep tableKey stable, no remount
    }
    if (langInitRef.current === i18n.language) return;
    langInitRef.current = i18n.language;
    setTableKey(i18n.language); // user-initiated switch: remount DataTable
    setRowState({});
    setLocalItems([]);
    childModeRef.current = {};
    childNodesRef.current = {};
    // WeakMap entries are GC'd with their row objects; no reset needed.
  }, [i18n.language, i18n.isInitialized]);

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
    <div style={{ opacity: isInitialized ? undefined : 0 }}>
      {saveError && (
        <Alert variant="danger" dismissible onClose={() => setSaveError(null)}>
          {saveError}
        </Alert>
      )}
      <DataTable
        key={tableKey}
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
              <th key={c.name ?? c.data ?? `col-${i}`}>{c.header}</th>
            ))}
            {hasActionsCol && <th>{actionsLabel}</th>}
          </tr>
        </thead>
      </DataTable>
      {pendingConfirm && (
        <Modal show onHide={() => setPendingConfirm(null)} centered>
          <Modal.Header closeButton>
            <Modal.Title>{t("confirm_delete_title")}</Modal.Title>
          </Modal.Header>
          <Modal.Body>{pendingConfirm.message}</Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setPendingConfirm(null)}>
              {t("cancel")}
            </Button>
            <Button variant="danger" onClick={pendingConfirm.onConfirm}>
              {t("delete")}
            </Button>
          </Modal.Footer>
        </Modal>
      )}
    </div>
  );
}
