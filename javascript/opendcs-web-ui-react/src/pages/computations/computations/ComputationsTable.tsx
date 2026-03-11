import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import { useTranslation } from "react-i18next";
import { Suspense, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { dtLangs } from "../../../lang";
import type { ApiComputation, ApiComputationRef } from "opendcs-api";
import Computation, { ComputationSkeleton, type UiComputation } from "./Computation";
import type { RemoveAction, SaveAction, UiState } from "../../../util/Actions";
import { useContextWrapper } from "../../../util/ContextWrapper";
import { Button } from "react-bootstrap";
import { Pencil, Trash } from "react-bootstrap-icons";
import type { RowState } from "../../../util/DataTables";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export type TableComputationRef = Partial<ApiComputationRef>;

export interface ComputationsTableProperties {
  computations: TableComputationRef[];
  getComputation?: (computationId: number) => Promise<ApiComputation>;
  actions?: SaveAction<ApiComputation> & RemoveAction<number>;
}

export const ComputationsTable: React.FC<ComputationsTableProperties> = ({
  computations,
  getComputation,
  actions = {},
}) => {
  const { toDom } = useContextWrapper();
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["computations", "translation"]);
  const [localComputations, updateLocalComputations] = useState<TableComputationRef[]>(
    [],
  );
  const [rowState, updateRowState] = useState<RowState<number>>({});
  const rowStateRef = useRef(rowState);
  const prevRowStateRef = useRef<RowState<number>>({});
  const localComputationsRef = useRef(localComputations);

  useEffect(() => {
    rowStateRef.current = rowState;
  }, [rowState]);

  useEffect(() => {
    localComputationsRef.current = localComputations;
  }, [localComputations]);

  const computationData = useMemo(
    () => [...computations, ...localComputations],
    [computations, localComputations],
  );

  const renderEnabled = useCallback((data: unknown, type: string) => {
    const enabled = Boolean(data);
    if (type !== "display") {
      return enabled ? 1 : 0;
    }
    return enabled ? "✓" : "";
  }, []);

  const columns = useMemo(
    () => [
      { data: "computationId", defaultContent: "new", className: "dt-left" },
      { data: "name", defaultContent: "" },
      { data: "algorithmName", defaultContent: "" },
      { data: "processName", defaultContent: "" },
      {
        data: "enabled",
        defaultContent: "",
        className: "dt-center",
        render: renderEnabled,
      },
      { data: "description", defaultContent: "" },
      { data: null, name: "actions" },
    ],
    [renderEnabled],
  );

  const renderActions = useCallback(
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (data: TableComputationRef, _row: any) => {
      const id = data.computationId;
      if (id === undefined) {
        return <></>;
      }

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
              aria-label={t("computations:editor.edit_for", { id })}
            >
              <Pencil />
            </Button>
          )}
          {!inEdit && id > 0 && (
            <Button
              variant="danger"
              aria-label={t("computations:editor.delete_for", { id })}
              onClick={(e) => {
                e.stopPropagation();
                actions.remove?.(id);
              }}
            >
              <Trash />
            </Button>
          )}
        </>
      );
    },
    [t, actions],
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
      top1Start: {
        buttons: [
          {
            text: "+",
            action: () => {
              updateLocalComputations((prev) => {
                const existing = prev
                  .map((v) => v.computationId!)
                  .sort((a, b) => b - a);
                const newId = existing.length > 0 ? existing[0] - 1 : -1;
                updateRowState((prevRowState) => ({
                  ...prevRowState,
                  [newId]: "new",
                }));
                return [...prev, { computationId: newId }];
              });
            },
            attr: { "aria-label": t("computations:add_computation") },
          },
        ],
      },
    },
  };

  const renderComputation = useCallback(
    (data: TableComputationRef, edit: boolean = false): Node => {
      const computationPromise: Promise<UiComputation> =
        data.computationId && data.computationId > 0
          ? getComputation!(data.computationId)
          : Promise.resolve({ computationId: data.computationId! } as UiComputation);

      const node = toDom(
        <Suspense fallback={<ComputationSkeleton edit={edit} />}>
          <Computation
            computation={computationPromise}
            actions={{
              save: (comp: ApiComputation) => {
                actions.save?.(comp);
                if (comp.computationId === undefined) {
                  return;
                }
                updateLocalComputations((prev) => [
                  ...prev.filter((pc) => pc.computationId !== comp.computationId),
                ]);
                updateRowState((prev) => {
                  const { [comp.computationId!]: _, ...states } = prev;
                  return { ...states, [comp.computationId!]: "show" };
                });
              },
              cancel: (item) => {
                const local = localComputationsRef.current.find(
                  (lc) => lc.computationId === item,
                );
                if (local) {
                  updateLocalComputations((prev) => [
                    ...prev.filter((plc) => plc.computationId !== item),
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
    [getComputation, toDom, actions],
  );

  useEffect(() => {
    updateRowState({});
    prevRowStateRef.current = {};
    updateLocalComputations([]);
  }, [i18n.language]);

  useEffect(() => {
    table.current
      ?.dt()
      ?.off("click")
      .on("click", "tbody tr.child-toggle", function (e) {
        if (getComputation === undefined) return;
        const target = e.target! as Element;
        const tr = target.closest("tr");
        if (tr?.classList.contains("child-row")) return;
        e.preventDefault();
        e.stopPropagation();
        const dt = table.current!.dt()!;
        const row = dt.row(tr as HTMLTableRowElement);
        updateRowState((prev) => {
          const idx = (row.data() as TableComputationRef).computationId!;
          const { [idx]: existing, ...remaining } = prev;
          let newValue: UiState = "show";
          if (existing !== undefined) {
            newValue = undefined;
          }
          return { ...remaining, [idx]: newValue };
        });
      });
  }, [i18n.language, getComputation]);

  useEffect(() => {
    if (table.current?.dt()) {
      const dt = table.current.dt()!;
      dt.rows({ page: "current", search: "applied" }).invalidate();
      dt.draw(false);
    }
  }, [computationData]);

  useEffect(() => {
    if (table.current?.dt()) {
      const dt = table.current.dt()!;
      const visibleRows = dt.rows({ page: "current", search: "applied" });
      let rowClosed = false;
      visibleRows.every(function () {
        const idx = (this.data() as TableComputationRef).computationId!;
        const currentState = rowStateRef.current[idx];
        const prevState = prevRowStateRef.current[idx];
        if (currentState === prevState) return;
        if (currentState !== undefined) {
          const data: TableComputationRef = this.data() as TableComputationRef;
          const edit = currentState !== "show";
          this.child(renderComputation(data, edit), "child-row").show();
        } else {
          this.child(false);
          rowClosed = true;
        }
      });
      prevRowStateRef.current = { ...rowStateRef.current };
      if (rowClosed) dt.draw(false);
    }
  }, [rowState, rowStateRef, renderComputation]);

  return (
    <DataTable
      key={i18n.language}
      id="computationTable"
      columns={columns}
      data={computationData}
      options={options}
      slots={slots}
      ref={table}
      className="table table-hover table-striped tablerow-cursor w-100 border"
    >
      <caption className="caption-title-center">
        {t("computations:computationsTitle")}
      </caption>
      <thead>
        <tr>
          <th>{t("computations:header.Id")}</th>
          <th>{t("computations:header.Name")}</th>
          <th>{t("computations:header.Algorithm")}</th>
          <th>{t("computations:header.Process")}</th>
          <th>{t("computations:header.Enabled")}</th>
          <th>{t("computations:header.Description")}</th>
          <th>{t("translation:actions")}</th>
        </tr>
      </thead>
    </DataTable>
  );
};
