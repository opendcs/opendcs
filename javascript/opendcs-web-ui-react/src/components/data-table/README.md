# `<AppDataTable>`

The project's wrapper around `datatables.net-react`. It owns every piece of
boilerplate that was repeated across list pages — Suspense + context wiring
for child rows, row-state machine, drawCallback sync, click delegation, row
actions with ARIA, add-new-local-row, pending-nav-after-save, language reset,
the processing overlay — so consumers supply only the data-model-specific
config.

## When to use it

- Any page that renders a list of domain objects in a DataTable.
- You get a simple flat read-only table, an expandable detail view, or an
  inline-editable table from the same component with different props.
- For truly custom cases, the `dataTableOptions` escape hatch lets you pass
  any raw DataTables option.

```tsx
import { AppDataTable, type ColumnDef } from "components/data-table";
```

---

## 30-second example: read-only list

```tsx
<AppDataTable<Site, number>
  data={sites}
  loading={stale}
  getId={(s) => s.siteId!}
  columns={[
    { data: "siteId", header: t("sites:header.id") },
    { data: "publicName", header: t("sites:header.name") },
    { data: "description", header: t("sites:header.description") },
  ]}
  caption={t("sites:title")}
  tableId="sitesTable"
/>
```

That's the whole table. The wrapper handles: DataTables setup, `key={i18n.language}`
remount on language change, `processing` overlay while `loading` is true, and the
table header/caption/thead markup.

---

## Core concepts

### 1. `data`, `getId`, and `columns` are required

- **`data: T[]`** — the array of row objects.
- **`getId: (row: T) => TId`** — returns a stable id; used to key row state
  and to build cell data for expansion/cache.
- **`columns: ColumnDef<T>[]`** — each column has `data` (key into `T`, or
  `null` for virtual columns) and `header` (what goes in the `<th>`). Other
  DataTables options (`defaultContent`, `className`, `name`, `orderable`,
  `searchable`, `render`) pass through.

### 2. Row modes

Rows internally live in a `"show" | "edit" | "new"` state machine. You
rarely set modes yourself — the wrapper drives them based on which feature
you enabled (row expansion, inline edit, add-new).

### 3. Two primary modes are **mutually exclusive**

- **Row expansion** — provide `renderDetail` + `renderSkeleton` → clicking a
  row toggles a child-row detail below it. The wrapper auto-applies
  `tablerow-cursor`.
- **Inline editing** — provide `inlineEdit` → cells with
  `ColumnDef.edit` swap between read and edit rendering; edit/save/cancel/delete
  buttons are auto-generated.

Setting both prints a dev warning; expansion wins.

---

## Feature: row actions

Declarative buttons rendered into the last cell, with click routing and
ARIA handled for you.

```tsx
rowActions={[
  {
    key: "open",
    icon: "bi-box-arrow-up-right",
    variant: "primary",
    aria: (row) => t("open_for", { name: row.name }),
    show: (row) => !row.archived,           // optional; hides button when false
    onClick: ({ row, rowEl, api }) => {
      // `rowEl` is the <tr>; handy when reading form values.
      // `api` exposes setMode/toggle/close for row modes.
      openExternal(row.id);
    },
  },
]}
```

Each button renders as:

```html
<button
  type="button"
  class="btn btn-warning btn-sm"
  data-row-action="edit"
  aria-label="..."
>
  <i class="bi bi-pencil"></i>
</button>
```

An Actions column is auto-appended to the table when `rowActions` (or
`inlineEdit`) is non-empty. Override the header via `actionsLabel` (defaults
to `"Actions"`).

---

## Feature: row expansion (list → detail)

Enables the classic "click the row to reveal a detail card below" pattern.

```tsx
<AppDataTable<AlgorithmRef, number, ApiAlgorithm>
  // ...core props as above
  rowActions={[...]}

  renderDetail={({ row, mode, actions }) => (
    <Algorithm
      algorithm={getAlgorithm(row.algorithmId!)}
      edit={mode !== "show"}
      actions={{
        save:   (algo) => actions.save(algo),   // triggers onSave + transitions to "show"
        cancel: ()     => actions.cancel(),     // closes the row, drops local new rows
      }}
    />
  )}
  renderSkeleton={({ mode }) => <AlgorithmSkeleton edit={mode !== "show"} />}

  onSave={saveAlgorithm}   // the wrapper awaits this (see below)
/>
```

### The third generic `TSave`

If your list row (`T`) and the shape committed by save (`TSave`) differ — e.g.
a list holds `AlgorithmRef` summaries but the detail commits a full
`ApiAlgorithm` — declare both:

```tsx
<AppDataTable<AlgorithmRef, number, ApiAlgorithm> ... />
```

`DetailActions<TSave>.save` is typed accordingly; so is `onSave`.

### `onSave` can be async

If `onSave` returns a `Promise`, the wrapper **awaits** it before transitioning
the row back to `"show"` mode. This avoids the classic save→refetch race where
the follow-up `GET` fires before the `POST` has committed on the server.

```tsx
onSave={(algo) =>
  algorithmApi.postAlgorithm(org, algo).then(() => setStale(true))
}
```

### `cancel` semantics

- Existing row in edit mode → closes the row, preserving unsaved data
  (the cached DOM node is discarded, so the next open rebuilds from the
  latest `data`).
- Locally-added new row → removes it from the local set and closes the row.

### Imperative: scroll-to-end after external refresh

When an _external_ action (e.g. an Import dialog) changes `data`, tell the
table to page to the end once the new `data` arrives:

```tsx
const tableRef = useRef<AppDataTableHandle>(null);

<AppDataTable ref={tableRef} ... />

<ImportModal onImported={() => {
  tableRef.current?.scheduleScrollToEnd();
  onRefresh();
}} />
```

Saves from inside a detail card schedule this automatically.

---

## Feature: inline editing

For tables where each row's cells become inputs in place (like the algorithm
parameters table).

### 1. Declare per-cell edit render + read

```tsx
const columns: ColumnDef<AlgoParm>[] = [
  {
    data: "roleName",
    header: t("roleName"),
    edit: {
      render: (row) =>
        renderToString(
          <Form.Control type="text" name="roleName" defaultValue={row.roleName} />,
        ),
      read: (cell) =>
        cell.querySelector<HTMLInputElement>('input[name="roleName"]')?.value.trim() ??
        "",
    },
  },
  {
    data: "parmType",
    header: t("parmType"),
    render: (_d, type, row) =>
      type === "display" ? parmTypeLabel(row.parmType) : row.parmType,
    edit: {
      render: (row) =>
        renderToString(
          <Form.Select name="parmType" defaultValue={row.parmType}>
            {PARM_TYPES.map((pt) => (
              <option key={pt.value} value={pt.value}>
                {pt.label}
              </option>
            ))}
          </Form.Select>,
        ),
      read: (cell) =>
        cell.querySelector<HTMLSelectElement>('select[name="parmType"]')?.value ?? "",
    },
  },
];
```

Each `edit.render` returns an **HTML string** (use `renderToString` for JSX).
Each `edit.read` is handed the `<td>` and extracts the user-entered value.

### 2. Wire up `inlineEdit`

```tsx
<AppDataTable<AlgoParm, string>
  data={parms}
  getId={(p) => p.roleName}
  columns={columns}
  inlineEdit={{
    onSave: (original, updated) => {
      if (!updated.roleName) return false; // invalid → stay in edit mode
      onUpdate?.(original.roleName, updated);
    },
    onAdd: (created) => {
      if (!created.roleName) return false;
      onAdd?.(created);
    },
    onRemove: (row) => onRemove?.(row.roleName),
    newTemplate: () => ({ roleName: "", parmType: "i" }),
    labels: {
      edit: (r) => t("edit_for", { name: r.roleName }),
      remove: (r) => t("delete_for", { name: r.roleName }),
      save: () => t("save"),
      cancel: () => t("cancel"),
      add: t("add"),
    },
  }}
/>
```

The wrapper then auto-generates the row-action buttons:

| Row mode           | Buttons shown                             |
| ------------------ | ----------------------------------------- |
| `"show"` (default) | `edit` (always), `delete` (if `onRemove`) |
| `"edit"` / `"new"` | `save`, `cancel`                          |

### 3. Save flow

When **save** is clicked the wrapper:

1. Walks the `<tr>`'s `<td>`s and calls `column.edit.read(cell)` on each
   column that has an `edit` spec.
2. Merges the read values into the row: `{ ...original, ...edits }`.
3. Calls `inlineEdit.onSave(original, updated)` (or `.onAdd(updated)` for
   new rows). Return `false` from either to keep the row in edit mode
   (validation failure).
4. On success: transitions the row back to `"show"` (and removes from local
   items if it was a new row).

### 4. `+` button for new rows

When `inlineEdit.newTemplate` is provided, the wrapper puts a `+` button in
the table's toolbar. Clicking it calls `newTemplate()` to build a fresh row,
stores it locally in `"new"` mode, and assigns it a synthetic id via an
internal `WeakMap` so it never collides with real ids — **your `getId`
doesn't need to handle this case**.

---

## Other props

### `addNew` (for non-inline-edit tables)

Adds a `+` button that creates a local row and tracks it via `rowState`.
Used by `renderDetail` flows where the detail itself provides the form
(e.g. `AlgorithmsTable`). Mutually exclusive with `inlineEdit.newTemplate`.

```tsx
addNew={{
  template: (nextId) => ({ algorithmId: nextId, /* … */ }),
  ariaLabel: t("add_algorithm"),
}}
```

The wrapper assigns `nextId` as a negative number less than any existing
local numeric id, so it's unique against server-side positive ids.

### `extraHeaderButtons`

Arbitrary buttons in the table toolbar (e.g. "Check for new", "Import…"):

```tsx
extraHeaderButtons={[
  { text: t("check_new"), ariaLabel: t("check_new"), onClick: () => setOpen(true) },
]}
```

### `loading`

When `true` the wrapper toggles DataTables' built-in `processing` overlay so
the empty table doesn't look like "no results". Pair with a `useState<T[]>([])`
initial value on the page — _don't_ `return null` while loading, because the
table shell needs to render once for the overlay to have something to cover.

### `caption`, `tableId`, `tableClassName`

Passed straight to the rendered table. `tableClassName` defaults to a sensible
`table table-hover table-striped w-100 border` (plus `tablerow-cursor` when
row expansion is enabled).

### `dataTableOptions` (escape hatch)

Merged into the wrapper's generated DataTables options. Use for `scrollY`,
`paging`, `order`, `lengthMenu`, etc.

```tsx
dataTableOptions={{
  paging: false,
  scrollY: "calc(10 * 2rem)",
  scrollCollapse: true,
}}
```

---

## Page-level conventions

These live outside the component but apply consistently across the app.

### 1. Initialize the list with `[]`, never `null`

```tsx
const [algorithms, setAlgorithms] = useState<ApiAlgorithmRef[]>([]);
```

Render the table unconditionally and pass `loading={stale}`. The wrapper
shows a processing overlay while the fetch runs, and rows appear as soon
as they arrive — avoids the blank-screen pause a `null` guard causes.

### 2. Return the save Promise from your page's save handler

```tsx
const saveAlgorithm = (algo: ApiAlgorithm): Promise<void> =>
  algorithmApi
    .postAlgorithm(org, algo)
    .then(() => setStale(true))
    .catch((e) => console.error(e));
```

The wrapper awaits this before re-rendering the detail, so the follow-up
`GET` sees committed data.

### 3. `onRefresh`

Any external refresh trigger (import modal, manual refresh button) should
call back into the page's `setStale(true)` — or the page's `onRefresh`
prop if you built one.

---

## Working examples in the codebase

| File                                                     | Pattern                                                                          |
| -------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `pages/computations/algorithms/AlgorithmsTable.tsx`      | Row expansion → `Algorithm` detail + `CheckForNewModal` via `extraHeaderButtons` |
| `pages/computations/algorithms/AlgorithmParamsTable.tsx` | Inline editing with `ColumnDef.edit` + `inlineEdit` config                       |
| `pages/computations/algorithms/index.tsx`                | Page-level conventions (`[]` initial, `loading`, `onSave` returns Promise)       |

Both component files are <150 lines — the wrapper absorbs the rest.

---

## Gotchas & limitations

- **`inlineEdit` reads cells by column index.** If you enable DataTables
  `responsive: true` and columns collapse into the DT child row, those reads
  won't find the form control. Either keep the table non-responsive or use
  fixed column widths.
- **`renderDetail` returns a React tree.** Its components can use React 19's
  `use()` to unwrap promises — the wrapper provides the Suspense boundary
  and `renderSkeleton` is its fallback.
- **Row identity for inline-edit new rows.** The wrapper tags new rows in a
  `WeakMap` with synthetic ids. Don't mutate new-row objects by reference
  after save — the WeakMap entry is cleaned up on commit, but replacing the
  object identity would orphan its mode state.
- **`dataTableOptions.layout`.** The wrapper builds `top1Start` for the
  `+` button and `extraHeaderButtons`. If you need `topStart` or `top2End`,
  supply them via `dataTableOptions.layout`; the wrapper merges but doesn't
  deep-merge `top1Start`, so don't set that key in your override.

---

## Related exports

| Export               | Use                                                                                                                                               |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| `DetailFade`         | Component that wraps a detail with a skeleton→content fade. Used inside the Algorithm detail card; not needed at the table level.                 |
| `useTableProcessing` | Low-level hook the wrapper uses internally. Exported for any custom DataTable that needs to drive the `processing` overlay from a `loading` flag. |
