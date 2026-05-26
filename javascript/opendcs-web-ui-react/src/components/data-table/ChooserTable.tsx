import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import {
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
import { useTableProcessing } from "./useTableProcessing";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

// =============================================================================
// Types
// =============================================================================

export interface ChooserColumnDef<T> {
  /** Row field to display, or `null` for a virtual/custom column. */
  data: (keyof T & string) | null;
  /** Header cell contents rendered inside the <th>. */
  header: ReactNode;
  defaultContent?: string;
  className?: string;
  name?: string;
  orderable?: boolean;
  searchable?: boolean;
  type?: "num" | "string" | "date" | "html" | "html-num" | "num-fmt";
  render?: (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    data: any,
    type: string,
    row: T,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    meta: any,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ) => any;
}

export type ChooserMode = "single" | "multi";

export interface ChooserTableHandle {
  /** Clear DataTables' search input and any column filters. */
  clearSearch: () => void;
  /** Set DataTables' global search value. */
  setSearch: (value: string) => void;
}

export interface ChooserTableProps<T, TId extends string | number> {
  // --- Data ---
  data: T[];
  loading?: boolean;
  getId: (row: T) => TId;

  // --- Columns ---
  columns: ChooserColumnDef<T>[];

  // --- Selection (controlled) ---
  /** `"single"` (default) highlights one row at a time; `"multi"` prepends a checkbox column. */
  mode?: ChooserMode;
  selectedIds: TId[];
  onSelectionChange: (ids: TId[]) => void;

  /** Optional fire-and-confirm on double-click — handy for "pick and close" UX. */
  onRowDoubleClick?: (row: T) => void;

  // --- Appearance ---
  caption?: ReactNode;
  tableId?: string;
  tableClassName?: string;
  /** Aria-label for the header "select all" checkbox (multi mode). */
  selectAllAriaLabel?: string;
  /** Aria-label generator for per-row select checkboxes (multi mode). */
  rowSelectAriaLabel?: (row: T) => string;

  // --- Escape hatch ---
  /** Merged into the generated DataTable options (user values win for simple keys). */
  dataTableOptions?: Partial<DataTableProps["options"]>;

  // --- Imperative handle ---
  ref?: React.Ref<ChooserTableHandle>;
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

// Minimal shape of the DataTables row API used in drawCallback.
type DtRowApi<T> = {
  node: () => Node;
  data: () => T | undefined;
};

const SELECT_COL_NAME = "__chooser_select";
const ROW_CLASS = "chooser-row";
const BASE_TABLE_CLASS = "table table-hover table-striped w-100 border tablerow-cursor";

// =============================================================================
// Component
// =============================================================================

/**
 * A focused selection table built on the same DataTables stack as
 * `AppDataTable`. Supports search / sort / pagination out of the box; renders
 * either a single-select (row-highlight) or multi-select (checkbox column)
 * picker. No expansion, no inline edit — just choosing.
 *
 * Selection is controlled: the caller owns `selectedIds` and receives updates
 * through `onSelectionChange`. The wrapping modal/page is responsible for the
 * Cancel / Confirm buttons.
 */
export function ChooserTable<T, TId extends string | number>(
  props: Readonly<ChooserTableProps<T, TId>>,
): React.ReactElement {
  const {
    data,
    loading = false,
    getId,
    columns,
    mode = "single",
    selectedIds,
    onSelectionChange,
    onRowDoubleClick,
    caption,
    tableId,
    tableClassName,
    selectAllAriaLabel = "Select all",
    rowSelectAriaLabel,
    dataTableOptions,
    ref,
  } = props;

  const tableRef = useRef<DataTableRef>(null);
  const [, i18n] = useTranslation();
  const [isInitialized, setIsInitialized] = useState(false);

  // --- Live refs so DataTables callbacks always read the latest values ------
  const selectedSet = useMemo(
    () => new Set(selectedIds.map((id) => String(id))),
    [selectedIds],
  );
  const selectedSetRef = useRef(selectedSet);
  useEffect(() => {
    selectedSetRef.current = selectedSet;
  }, [selectedSet]);

  const getIdRef = useRef(getId);
  useEffect(() => {
    getIdRef.current = getId;
  }, [getId]);

  const modeRef = useRef(mode);
  useEffect(() => {
    modeRef.current = mode;
  }, [mode]);

  const rowSelectAriaLabelRef = useRef(rowSelectAriaLabel);
  useEffect(() => {
    rowSelectAriaLabelRef.current = rowSelectAriaLabel;
  }, [rowSelectAriaLabel]);

  // Per-row data lookup keyed on the <tr> element, for click delegation.
  const rowDataRef = useRef<WeakMap<HTMLTableRowElement, T>>(new WeakMap());

  // --- Selection helpers ----------------------------------------------------
  // Reorder a set of selected id-strings into the original `data` order so
  // callers receive a stable, predictable array (selecting "all" never returns
  // a randomised list).
  const orderSelected = useCallback(
    (idStrs: Set<string>): TId[] => {
      const out: TId[] = [];
      const seen = new Set<string>();
      data.forEach((r) => {
        const idStr = String(getIdRef.current(r));
        if (idStrs.has(idStr) && !seen.has(idStr)) {
          out.push(getIdRef.current(r));
          seen.add(idStr);
        }
      });
      return out;
    },
    [data],
  );

  const toggleOne = useCallback(
    (rowData: T) => {
      const idStr = String(getIdRef.current(rowData));
      if (modeRef.current === "single") {
        if (selectedSetRef.current.has(idStr)) {
          onSelectionChange([]);
        } else {
          onSelectionChange([getIdRef.current(rowData)]);
        }
        return;
      }
      const next = new Set(selectedSetRef.current);
      if (next.has(idStr)) next.delete(idStr);
      else next.add(idStr);
      onSelectionChange(orderSelected(next));
    },
    [onSelectionChange, orderSelected],
  );

  const toggleSelectAll = useCallback(
    (checked: boolean) => {
      const dt = tableRef.current?.dt();
      if (!dt) return;
      const next = new Set(selectedSetRef.current);
      dt.rows({ search: "applied" }).every(function () {
        const rd = (this as unknown as DtRowApi<T>).data();
        if (rd == null) return;
        const idStr = String(getIdRef.current(rd));
        if (checked) next.add(idStr);
        else next.delete(idStr);
      });
      onSelectionChange(orderSelected(next));
    },
    [onSelectionChange, orderSelected],
  );

  // --- Imperative handle ----------------------------------------------------
  useImperativeHandle(
    ref,
    () => ({
      clearSearch: () => {
        tableRef.current?.dt()?.search("").draw(false);
      },
      setSearch: (value: string) => {
        tableRef.current?.dt()?.search(value).draw(false);
      },
    }),
    [],
  );

  // --- Columns --------------------------------------------------------------
  const dtColumns = useMemo(() => {
    const cols = columns.map((c) => ({
      data: c.data,
      defaultContent: c.defaultContent ?? "",
      className: c.className,
      name: c.name,
      orderable: c.orderable,
      // Two-step asc ↔ desc cycle; DataTables' default third click removes
      // ordering entirely, which is confusing in a chooser.
      orderSequence: ["asc", "desc"] as Array<"asc" | "desc" | "">,
      searchable: c.searchable,
      type: c.type,
      render: c.render,
    }));
    if (mode === "multi") {
      cols.unshift({
        data: null,
        defaultContent: "",
        className: "chooser-select-cell text-center",
        name: SELECT_COL_NAME,
        orderable: false,
        // orderSequence is meaningless on a non-orderable column, but the
        // shared shape of the cols array carries the same field on every
        // entry — leave a benign value. The real guarantee that DataTables
        // skips this header is the data-dt-order="disable" on the <th>.
        orderSequence: ["asc", "desc"] as Array<"asc" | "desc" | "">,
        searchable: false,
        type: undefined,
        render: undefined,
      });
    }
    return cols;
  }, [columns, mode]);

  // --- DataTable options ----------------------------------------------------
  const options: DataTableProps["options"] = {
    paging: true,
    // Choosers are narrow, focused tables with a handful of columns; the
    // responsive plugin can misread width (especially inside a scroll wrapper)
    // and silently collapse columns, which makes their headers unclickable and
    // looks like sort is broken. Callers can opt in via `dataTableOptions`.
    responsive: false,
    stateSave: false,
    processing: true,
    deferRender: true,
    language: dtLangs.get(i18n.language),
    ...dataTableOptions,
    initComplete: () => setIsInitialized(true),
    createdRow: (row) => {
      (row as HTMLElement).classList.add(ROW_CLASS);
    },
    drawCallback: function () {
      const dt = this.api(); // NOSONAR — DataTables binds `this` to the Api in drawCallback
      const set = selectedSetRef.current;
      const currentMode = modeRef.current;

      dt.rows({ page: "current", search: "applied" }).every(function () {
        const rowApi = this as unknown as DtRowApi<T>;
        const rowData = rowApi.data();
        if (rowData == null) return;
        const rowEl = rowApi.node() as HTMLTableRowElement;
        rowDataRef.current.set(rowEl, rowData);
        const idStr = String(getIdRef.current(rowData));
        const isSelected = set.has(idStr);
        rowEl.classList.toggle("table-primary", isSelected && currentMode === "single");
        rowEl.setAttribute("aria-selected", String(isSelected));
        if (currentMode === "multi") {
          const cell = rowEl.querySelector<HTMLElement>("td.chooser-select-cell");
          if (cell) {
            const aria = rowSelectAriaLabelRef.current
              ? rowSelectAriaLabelRef.current(rowData)
              : "Select row";
            const html =
              `<input type="checkbox" class="form-check-input chooser-row-check"` +
              (isSelected ? " checked" : "") +
              ` aria-label="${escapeHtml(aria)}" tabindex="-1" />`;
            if (cell.innerHTML !== html) cell.innerHTML = html;
          }
        }
      });

      // Header "select all" checkbox state (multi only).
      if (currentMode === "multi") {
        const headerEl = dt
          .column(`${SELECT_COL_NAME}:name`)
          .header() as HTMLElement | null;
        const cb = headerEl?.querySelector<HTMLInputElement>(
          "input.chooser-select-all",
        );
        if (cb) {
          let total = 0;
          let selected = 0;
          dt.rows({ search: "applied" }).every(function () {
            const rd = (this as unknown as DtRowApi<T>).data();
            if (rd == null) return;
            total++;
            if (set.has(String(getIdRef.current(rd)))) selected++;
          });
          cb.checked = total > 0 && selected === total;
          cb.indeterminate = selected > 0 && selected < total;
        }
      }
    },
  };

  // --- Click delegation: row click + optional double-click ------------------
  useEffect(() => {
    const dt = tableRef.current?.dt();
    if (!dt) return;

    dt.off("click.chooser dblclick.chooser");

    dt.on("click.chooser", `tbody tr.${ROW_CLASS}`, function (e) {
      const target = e.target as Element;
      const tr = target.closest("tr") as HTMLTableRowElement | null;
      if (!tr || tr.classList.contains("child-row")) return;
      const rowData = rowDataRef.current.get(tr);
      if (!rowData) return;
      e.preventDefault();
      e.stopPropagation();
      // The row's select-checkbox grabs focus on click, then drawCallback
      // rewrites the cell's innerHTML — the new input doesn't inherit focus,
      // but the *old* one's focus ring was painted before the swap and can
      // linger. Blur explicitly before redrawing so the ring goes away.
      const focused = document.activeElement;
      if (focused instanceof HTMLElement && tr.contains(focused)) {
        focused.blur();
      }
      toggleOne(rowData);
    });

    if (onRowDoubleClick) {
      dt.on("dblclick.chooser", `tbody tr.${ROW_CLASS}`, function (e) {
        const tr = (e.target as Element).closest("tr") as HTMLTableRowElement | null;
        if (!tr) return;
        const rowData = rowDataRef.current.get(tr);
        if (!rowData) return;
        e.preventDefault();
        e.stopPropagation();
        onRowDoubleClick(rowData);
      });
    }
  }, [i18n.language, toggleOne, onRowDoubleClick]);

  // --- Header "select all" change handler (multi mode) ----------------------
  useEffect(() => {
    if (mode !== "multi") return;
    const dt = tableRef.current?.dt();
    if (!dt) return;
    const headerEl = dt
      .column(`${SELECT_COL_NAME}:name`)
      .header() as HTMLElement | null;
    if (!headerEl) return;
    const handler = (e: Event) => {
      const target = e.target as HTMLInputElement | null;
      if (!target?.classList.contains("chooser-select-all")) return;
      toggleSelectAll(target.checked);
    };
    headerEl.addEventListener("change", handler);
    return () => headerEl.removeEventListener("change", handler);
  }, [mode, toggleSelectAll, i18n.language, isInitialized]);

  // --- Force redraw when selection changes so highlights + checkboxes sync --
  useEffect(() => {
    tableRef.current?.dt()?.draw(false);
  }, [selectedSet]);

  // --- Processing overlay while loading ------------------------------------
  useTableProcessing(tableRef, loading);

  return (
    <div style={{ opacity: isInitialized ? undefined : 0 }}>
      <DataTable
        key={i18n.language}
        id={tableId}
        columns={dtColumns}
        data={data}
        options={options}
        ref={tableRef}
        className={tableClassName ?? BASE_TABLE_CLASS}
      >
        {caption && <caption className="caption-title-center">{caption}</caption>}
        <thead>
          <tr>
            {mode === "multi" && (
              <th className="chooser-select-cell text-center" data-dt-order="disable">
                <input
                  type="checkbox"
                  className="form-check-input chooser-select-all"
                  aria-label={selectAllAriaLabel}
                />
              </th>
            )}
            {columns.map((c, i) => (
              <th key={c.name ?? c.data ?? `col-${i}`}>{c.header}</th>
            ))}
          </tr>
        </thead>
      </DataTable>
    </div>
  );
}
