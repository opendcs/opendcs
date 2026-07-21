import { useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import type { ApiAlgorithm, ApiAlgorithmRef, ApiPropSpec } from "opendcs-api";
import Algorithm, { AlgorithmSkeleton, type UiAlgorithm } from "./Algorithm";
import type { AlgoParm } from "./AlgorithmParamsTable";
import { CheckForNewModal } from "./CheckForNewModal";
import type { RemoveAction, SaveAction } from "../../../util/Actions";
import {
  AppDataTable,
  type AppDataTableHandle,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";

export type TableAlgorithmRef = Partial<ApiAlgorithmRef>;

export interface AlgorithmsTableProperties {
  algorithms: TableAlgorithmRef[];
  getAlgorithm?: (algorithmId: number) => Promise<ApiAlgorithm>;
  getPropSpecs?: (execClass: string) => Promise<ApiPropSpec[]>;
  actions?: SaveAction<ApiAlgorithm> & RemoveAction<number>;
  loading?: boolean;
}

const toTableRef = (algo: UiAlgorithm): TableAlgorithmRef => ({
  algorithmId: algo.algorithmId,
  algorithmName: algo.name,
  execClass: algo.execClass,
  numCompsUsing: 0,
  description: algo.description,
});

const copiedAlgorithm = (source: ApiAlgorithm, newId: number): UiAlgorithm => ({
  ...source,
  algorithmId: newId,
  name: "",
  numCompsUsing: 0,
});

export const AlgorithmsTable: React.FC<AlgorithmsTableProperties> = ({
  algorithms,
  getAlgorithm,
  getPropSpecs,
  actions = {},
  loading = false,
}) => {
  const [t] = useTranslation(["algorithms", "translation"]);
  const [showCheckNew, setShowCheckNew] = useState(false);
  const tableRef = useRef<AppDataTableHandle<TableAlgorithmRef>>(null);
  const draftsRef = useRef<Record<number, UiAlgorithm>>({});

  const columns = useMemo<ColumnDef<TableAlgorithmRef>[]>(
    () => [
      {
        data: "algorithmId",
        header: t("algorithms:header.Id"),
        defaultContent: "new",
        className: "dt-left",
        type: "num",
      },
      { data: "algorithmName", header: t("algorithms:header.Name"), type: "string" },
      { data: "execClass", header: t("algorithms:header.ExecClass"), type: "string" },
      {
        data: "numCompsUsing",
        header: t("algorithms:header.NumCompsUsing"),
        className: "dt-left",
        type: "num",
      },
      {
        data: "description",
        header: t("algorithms:header.Description"),
        type: "string",
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<TableAlgorithmRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("algorithms:editor.edit_for", { id: row.algorithmId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "copy",
        icon: "bi-files",
        variant: "info",
        show: (row) => (row.algorithmId ?? 0) > 0,
        aria: (row) => t("algorithms:editor.copy_for", { id: row.algorithmId }),
        onClick: async ({ row }) => {
          if (!getAlgorithm || !row.algorithmId) return;
          try {
            const source = await getAlgorithm(row.algorithmId);
            tableRef.current?.appendLocalItem((newId) => {
              const draft = copiedAlgorithm(source, newId);
              draftsRef.current[newId] = draft;
              return toTableRef(draft);
            }, "new");
          } catch (err) {
            console.warn(`Failed to copy algorithm ${row.algorithmId}`, err);
          }
        },
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        show: (row) => (row.algorithmId ?? 0) > 0,
        aria: (row) => t("algorithms:editor.delete_for", { id: row.algorithmId }),
        onClick: ({ row }) => {
          if (row.algorithmId !== undefined) actions.remove?.(row.algorithmId);
        },
      },
    ],
    [t, getAlgorithm, actions],
  );

  return (
    <>
      <AppDataTable<TableAlgorithmRef, number, ApiAlgorithm>
        ref={tableRef}
        data={algorithms}
        loading={loading}
        getId={(a) => a.algorithmId!}
        columns={columns}
        actionsLabel={t("translation:actions")}
        rowActions={rowActions}
        renderDetail={({ row, mode, actions: detailActions }) => {
          const algorithmPromise: Promise<UiAlgorithm> =
            row.algorithmId && row.algorithmId > 0 && getAlgorithm
              ? getAlgorithm(row.algorithmId)
              : Promise.resolve(
                  draftsRef.current[row.algorithmId ?? 0] ??
                    ({ algorithmId: row.algorithmId } as UiAlgorithm),
                );
          const parmsPromise: Promise<AlgoParm[]> = algorithmPromise.then(
            (algo) => (algo.parms ?? []) as AlgoParm[],
          );
          const propSpecs: Promise<ApiPropSpec[]> =
            getPropSpecs && row.execClass
              ? getPropSpecs(row.execClass)
              : Promise.resolve([]);
          return (
            <Algorithm
              algorithm={algorithmPromise}
              propSpecs={propSpecs}
              initialParms={parmsPromise}
              actions={{
                save: (algo) => detailActions.save(algo),
                cancel: () => detailActions.cancel(),
              }}
              edit={mode !== "show"}
            />
          );
        }}
        renderSkeleton={({ mode }) => (
          <AlgorithmSkeleton edit={mode !== "show"} className="child-row-opening" />
        )}
        addNew={{
          template: (id) => ({ algorithmId: id }),
          ariaLabel: t("algorithms:add_algorithm"),
        }}
        extraHeaderButtons={[
          {
            text: t("algorithms:check_new.button"),
            ariaLabel: t("algorithms:check_new.button"),
            onClick: () => setShowCheckNew(true),
          },
        ]}
        onSave={actions.save}
        caption={t("algorithms:algorithmsTitle")}
        tableId="algorithmTable"
      />
      <CheckForNewModal
        show={showCheckNew}
        onHide={() => setShowCheckNew(false)}
        onImported={() => tableRef.current?.scheduleScrollToEnd()}
      />
    </>
  );
};
