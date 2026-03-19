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
import type { AlgoParm } from "./AlgorithmParamsTable";
import { CheckForNewModal } from "./CheckForNewModal";
import type { RemoveAction, SaveAction, UiState } from "../../../util/Actions";
import { useContextWrapper } from "../../../util/ContextWrapper";
import { Button } from "react-bootstrap";
import { Pencil, Trash } from "react-bootstrap-icons";
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
}

export const AlgorithmsTable: React.FC<AlgorithmsTableProperties> = ({
  algorithms,
  getAlgorithm,
  getPropSpecs,
  actions = {},
  onRefresh,
}) => {
  const { toDom } = useContextWrapper();
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["algorithms", "translation"]);
  const [localAlgorithms, updateLocalAlgorithms] = useState<TableAlgorithmRef[]>([]);
  const [rowState, updateRowState] = useState<RowState<number>>({});
  const rowStateRef = useRef(rowState);
  const prevRowStateRef = useRef<RowState<number>>({});
  const localAlgorithmsRef = useRef(localAlgorithms);
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
    { data: null, name: "actions" },
  ];

  const renderActions = useCallback(
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (data: TableAlgorithmRef, _row: any) => {
      const id: number = data.algorithmId!;
      const curRowState = rowStateRef.current[id];
      const inEdit = curRowState === "edit" || curRowState === "new";
      return (
        <>
          {!inEdit && (
            <Button
              variant="warning"
              onClick={(e) => {
                e.stopPropagation();
                updateRowState((prev) => ({ ...prev, [id]: "edit" }));
              }}
              aria-label={t("algorithms:editor.edit_for", { id })}
            >
              <Pencil />
            </Button>
          )}
          {!inEdit && data.algorithmId! > 0 && (
            <Button
              variant="danger"
              aria-label={t("algorithms:editor.delete_for", { id: data.algorithmId })}
              onClick={(e) => {
                e.stopPropagation();
                actions.remove!(id);
              }}
            >
              <Trash />
            </Button>
          )}
        </>
      );
    },
    [t, actions.remove],
  );

  const slots = { actions: renderActions };

  const options: DataTableProps["options"] = {
    paging: true,
    responsive: true,
    stateSave: true,
    language: dtLangs.get(i18n.language),
    createdRow: (_row, _data, dataIndex) => {
      table.current?.dt()?.row(dataIndex).node().classList.add("child-toggle");
    },
    layout: {
      top1Start: [
        {
          buttons: [
            {
              text: "+",
              action: () => {
                updateLocalAlgorithms((prev) => {
                  const existing = prev
                    .map((v) => v.algorithmId!)
                    .sort((a, b) => b - a);
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
        <Suspense fallback={<AlgorithmSkeleton edit={edit} />}>
          <Algorithm
            algorithm={algorithmPromise}
            propSpecs={propSpecs}
            initialParms={parmsPromise}
            actions={{
              save: (algo: ApiAlgorithm) => {
                actions.save!(algo);
                updateLocalAlgorithms((prev) => [
                  ...prev.filter((pa) => pa.algorithmId !== algo.algorithmId),
                ]);
                updateRowState((prev) => {
                  const { [algo.algorithmId!]: _, ...states } = prev;
                  return { ...states, [algo.algorithmId!]: "show" };
                });
              },
              cancel: (item) => {
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
      const el = node as HTMLElement;
      el.classList.add("child-row-opening");
      el.addEventListener(
        "animationend",
        () => el.classList.remove("child-row-opening"),
        { once: true },
      );
      return node;
    },
    [getAlgorithm, getPropSpecs, toDom, actions.save],
  );

  // When the language changes, the DataTable remounts (key={i18n.language}).
  // Reset row and local state so stale edit/new flags don't hide action buttons.
  useEffect(() => {
    updateRowState({});
    prevRowStateRef.current = {};
    updateLocalAlgorithms([]);
  }, [i18n.language]);

  useEffect(() => {
    table.current
      ?.dt()
      ?.off("click")
      .on("click", "tbody tr.child-toggle", function (e) {
        if (getAlgorithm === undefined) return;
        const target = e.target! as Element;
        const tr = target.closest("tr");
        if (tr?.classList.contains("child-row")) return;
        e.preventDefault();
        e.stopPropagation();
        const dt = table.current!.dt()!;
        const row = dt.row(tr as HTMLTableRowElement);
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
  }, [i18n.language, getAlgorithm]);

  // Safe wrapper around dt.draw() — DataTable's internal _fnScrollDraw can
  // throw when row nodes have been removed but its internal index is stale
  // (e.g. after deleting the last row).  Swallowing the error is safe
  // because the table will redraw correctly on the next React render cycle.
  const safeDraw = useCallback(
    (dt: ReturnType<NonNullable<DataTableRef["dt"]>> | null) => {
      if (!dt) return;
      try {
        dt.draw(true);
      } catch {
        // DataTable internal error during draw — ignore
      }
    },
    [],
  );

  // Redraw only when the underlying data changes, not on row open/close.
  // Separating this from the child-row effect prevents the action buttons
  // from flickering every time a row is expanded or collapsed.
  useEffect(() => {
    const dt = table.current?.dt();
    if (!dt) return;
    const rows = dt.rows({ page: "current", search: "applied" });
    if (rows.count() > 0) {
      rows.invalidate();
    }
    safeDraw(dt);
  }, [algorithmData, safeDraw]);

  useEffect(() => {
    if (table.current?.dt()) {
      const dt = table.current.dt()!;
      const visibleRows = dt.rows({ page: "current", search: "applied" });
      let rowClosed = false;
      visibleRows.every(function () {
        const rowData = this.data() as TableAlgorithmRef | undefined;
        if (!rowData) return;
        const idx = rowData.algorithmId!;
        const currentState = rowStateRef.current[idx];
        const prevState = prevRowStateRef.current[idx];
        if (currentState === prevState) return; // state unchanged — skip re-render
        if (currentState !== undefined) {
          const data: TableAlgorithmRef = this.data() as TableAlgorithmRef;
          const edit = currentState !== "show";
          this.child(renderAlgorithm(data, edit), "child-row").show();
        } else {
          // child(false) fully removes the <tr> from the DOM and clears
          // DataTables' internal child state, so CSS nth-child striping
          // recalculates correctly without needing a full table redraw.
          this.child(false);
          rowClosed = true;
        }
      });
      prevRowStateRef.current = { ...rowStateRef.current };
      if (rowClosed) safeDraw(dt);
    }
  }, [rowState, rowStateRef, renderAlgorithm, safeDraw]);

  return (
    <>
      <DataTable
        key={i18n.language}
        id="algorithmTable"
        columns={columns}
        data={algorithmData}
        options={options}
        slots={slots}
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
        onImported={() => onRefresh?.()}
      />
    </>
  );
};
