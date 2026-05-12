import { useCallback, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import type {
  ApiAlgorithm,
  ApiAppRef,
  ApiComputation,
  ApiComputationRef,
  ApiTsGroupRef,
} from "opendcs-api";
import Computation, { ComputationSkeleton, type UiComputation } from "./Computation";
import type { RemoveAction, SaveAction } from "../../../util/Actions";
import {
  AppDataTable,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";

export type TableComputationRef = Partial<ApiComputationRef>;

export interface ComputationsTableProperties {
  computations: TableComputationRef[];
  loading?: boolean;
  getComputation?: (computationId: number) => Promise<ApiComputation>;
  getAlgorithm?: (algorithmId: number) => Promise<ApiAlgorithm>;
  actions?: SaveAction<ApiComputation> & RemoveAction<number>;
  processOptions?: ApiAppRef[];
  groupOptions?: ApiTsGroupRef[];
}

// Output parms/props belong to the algorithm's output role and are not copied
// because they would produce duplicate output bindings on the new computation.
const isOutputParm = (parm: { algoParmType?: string }): boolean =>
  (parm.algoParmType ?? "").trim().toLowerCase().startsWith("o");

const isOutputProp = (propName: string): boolean =>
  propName.trim().toLowerCase().startsWith("output");

const toTableRef = (comp: UiComputation): TableComputationRef => ({
  computationId: comp.computationId,
  name: comp.name,
  algorithmId: comp.algorithmId,
  algorithmName: comp.algorithmName,
  processId: comp.appId,
  processName: comp.applicationName,
  enabled: comp.enabled,
  description: comp.comment,
  groupId: comp.groupId,
  groupName: comp.groupName,
});

const copiedComputation = (source: ApiComputation, newId: number): UiComputation => ({
  ...source,
  computationId: newId,
  name: "",
  props: Object.fromEntries(
    Object.entries(source.props ?? {}).filter(([key]) => !isOutputProp(key)),
  ),
  parmList: (source.parmList ?? [])
    .filter((parm) => !isOutputParm(parm))
    .map((parm) => ({ ...parm })),
});

export const ComputationsTable: React.FC<ComputationsTableProperties> = ({
  computations,
  loading = false,
  getComputation,
  getAlgorithm,
  actions = {},
  processOptions = [],
  groupOptions = [],
}) => {
  const [t] = useTranslation(["computations", "translation"]);
  // Copies are managed externally so AppDataTable receives them as regular data
  // rows. The user clicks the row to open it, same as any existing computation.
  const [localComputations, setLocalComputations] = useState<TableComputationRef[]>([]);
  const draftsRef = useRef<Record<number, UiComputation>>({});

  const renderEnabled = useCallback((data: unknown, type: string) => {
    const enabled = Boolean(data);
    if (type !== "display") return enabled ? 1 : 0;
    return enabled ? "✓" : "";
  }, []);

  const columns = useMemo<ColumnDef<TableComputationRef>[]>(
    () => [
      {
        data: "computationId",
        header: t("computations:header.Id"),
        defaultContent: "new",
        className: "dt-left",
      },
      { data: "name", header: t("computations:header.Name") },
      { data: "algorithmName", header: t("computations:header.Algorithm") },
      { data: "processName", header: t("computations:header.Process") },
      {
        data: "enabled",
        header: t("computations:header.Enabled"),
        defaultContent: "",
        className: "dt-center",
        render: renderEnabled,
      },
      { data: "description", header: t("computations:header.Description") },
    ],
    [t, renderEnabled],
  );

  const rowActions = useMemo<RowAction<TableComputationRef>[]>(
    () => [
      {
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("computations:editor.edit_for", { id: row.computationId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
      },
      {
        key: "copy",
        icon: "bi-files",
        variant: "info",
        show: (row) => (row.computationId ?? 0) > 0,
        aria: (row) => t("computations:editor.copy_for", { id: row.computationId }),
        onClick: async ({ row }) => {
          if (!getComputation || !row.computationId) return;
          try {
            const source = await getComputation(row.computationId);
            const newId = -Date.now();
            const draft = copiedComputation(source, newId);
            draftsRef.current[newId] = draft;
            setLocalComputations((prev) => [...prev, toTableRef(draft)]);
          } catch (err) {
            console.warn(`Failed to copy computation ${row.computationId}`, err);
          }
        },
      },
      {
        key: "delete",
        icon: "bi-trash",
        variant: "danger",
        show: (row) => (row.computationId ?? 0) > 0,
        aria: (row) => t("computations:editor.delete_for", { id: row.computationId }),
        onClick: ({ row }) => {
          if (row.computationId !== undefined) actions.remove?.(row.computationId);
        },
      },
    ],
    [t, getComputation, actions],
  );

  const allComputations = useMemo(
    () => [...computations, ...localComputations],
    [computations, localComputations],
  );

  return (
    <AppDataTable<TableComputationRef, number, ApiComputation>
      data={allComputations}
      loading={loading}
      getId={(c) => c.computationId!}
      columns={columns}
      actionsLabel={t("translation:actions")}
      rowActions={rowActions}
      renderDetail={({ row, mode, actions: detailActions }) => {
        const computationId = row.computationId!;
        const computationPromise: Promise<UiComputation> =
          computationId > 0 && getComputation
            ? getComputation(computationId)
            : Promise.resolve(
                draftsRef.current[computationId] ??
                  ({ computationId } as UiComputation),
              );

        const algorithmPromise: Promise<ApiAlgorithm | undefined> =
          computationPromise.then(async (comp) => {
            if (!getAlgorithm || !comp.algorithmId || comp.algorithmId <= 0) {
              return undefined;
            }
            try {
              return await getAlgorithm(comp.algorithmId);
            } catch (err) {
              console.warn(
                `Failed to load algorithm for algorithmId=${comp.algorithmId}`,
                err,
              );
              return undefined;
            }
          });

        return (
          <Computation
            computation={computationPromise}
            algorithm={algorithmPromise}
            actions={{
              save: (comp) => detailActions.save(comp),
              cancel: () => detailActions.cancel(),
            }}
            edit={mode !== "show"}
            processOptions={processOptions}
            groupOptions={groupOptions}
          />
        );
      }}
      renderSkeleton={({ mode }) => <ComputationSkeleton edit={mode !== "show"} />}
      addNew={{
        template: (id) => ({ computationId: id }),
        ariaLabel: t("computations:add_computation"),
      }}
      onSave={actions.save}
      caption={t("computations:computationsTitle")}
      tableId="computationTable"
    />
  );
};
