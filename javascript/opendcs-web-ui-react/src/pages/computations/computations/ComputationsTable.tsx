import { useCallback, useMemo, useRef, useState } from "react";
import RunComputationModal from "./RunComputationModal";
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
  type AppDataTableHandle,
  type ColumnDef,
  type RowAction,
} from "../../../components/data-table";
import { AlgorithmSelectModal } from "./AlgorithmSelectModal";

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

const isOutputParm = (parm: { algoParmType?: string }): boolean =>
  (parm.algoParmType ?? "").trim().toLowerCase().startsWith("o");

const isOutputProp = (propName: string): boolean =>
  propName.trim().toLowerCase().startsWith("output");

const descriptionSnippet = (description: string | undefined): string => {
  const normalized = (description ?? "").replace(/\s+/g, " ").trim();
  if (normalized.length <= 120) return normalized;
  return `${normalized.slice(0, 117).trimEnd()}...`;
};

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
  const [showCheckNew, setShowCheckNew] = useState(false);
  const [runTarget, setRunTarget] = useState<{
    id: number;
    name: string | undefined;
    groupId: number | undefined;
  } | null>(null);
  const tableRef = useRef<AppDataTableHandle<TableComputationRef>>(null);
  const draftsRef = useRef<Record<number, UiComputation>>({});

  const renderDescription = useCallback((data: unknown, type: string) => {
    const description = typeof data === "string" ? data : "";
    if (type !== "display") return description;
    return descriptionSnippet(description);
  }, []);

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
        type: "num",
      },
      { data: "name", header: t("computations:header.Name"), type: "string" },
      {
        data: "algorithmName",
        header: t("computations:header.Algorithm"),
        type: "string",
      },
      { data: "processName", header: t("computations:header.Process"), type: "string" },
      {
        data: "enabled",
        header: t("computations:header.Enabled"),
        defaultContent: "",
        className: "dt-center",
        type: "num",
        render: renderEnabled,
      },
      {
        data: "description",
        header: t("computations:header.Description"),
        type: "string",
        render: renderDescription,
      },
    ],
    [t, renderEnabled, renderDescription],
  );

  const rowActions = useMemo<RowAction<TableComputationRef>[]>(
    () => [
      {
        key: "run",
        icon: "bi-play-fill",
        variant: "success",
        show: (row) => (row.computationId ?? 0) > 0,
        aria: (row) => t("computations:run.run_for", { id: row.computationId }),
        onClick: ({ row }) => {
          if (row.computationId !== undefined) {
            setRunTarget({
              id: row.computationId,
              name: row.name,
              groupId: row.groupId ?? undefined,
            });
          }
        },
      },
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
            tableRef.current?.appendLocalItem((newId) => {
              const draft = copiedComputation(source, newId);
              draftsRef.current[newId] = draft;
              return toTableRef(draft);
            }, "new");
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
        confirm: () => t("translation:confirm_delete_prompt"),
        onClick: ({ row }) => {
          if (row.computationId !== undefined) actions.remove?.(row.computationId);
        },
      },
    ],
    [t, getComputation, actions],
  );

  return (
    <>
      <AppDataTable<TableComputationRef, number, ApiComputation>
        ref={tableRef}
        data={computations}
        loading={loading}
        getId={(c) => c.computationId!}
        columns={columns}
        actionsLabel={t("translation:actions")}
        rowActions={rowActions}
        extraHeaderButtons={[
          {
            text: t("computations:add_from_algorithms.button"),
            ariaLabel: t("computations:add_from_algorithms.button"),
            onClick: () => setShowCheckNew(true),
          },
        ]}
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
              getAlgorithm={getAlgorithm}
              actions={{
                save: detailActions.save,
                cancel: detailActions.cancel,
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
      <RunComputationModal
        show={runTarget !== null}
        computationId={runTarget?.id}
        computationName={runTarget?.name}
        groupId={runTarget?.groupId}
        onHide={() => setRunTarget(null)}
      />
      <AlgorithmSelectModal
        show={showCheckNew}
        onHide={() => setShowCheckNew(false)}
        onSelect={async (algo) => {
          let draft: UiComputation = {
            algorithmId: algo.algorithmId,
            algorithmName: algo.algorithmName,
          };
          if (getAlgorithm && algo.algorithmId) {
            try {
              const fullAlgo = await getAlgorithm(algo.algorithmId);
              draft = {
                ...draft,
                algorithmName: fullAlgo.name ?? algo.algorithmName,
                comment: fullAlgo.description,
                props: fullAlgo.props ?? {},
                parmList: (fullAlgo.parms ?? [])
                  .filter((p) => (p.roleName ?? "").trim().length > 0)
                  .map((p) => ({
                    algoRoleName: p.roleName,
                    algoParmType: p.parmType,
                  })),
              };
            } catch (err) {
              console.warn(
                `Failed to fetch algorithm ${algo.algorithmId} for add`,
                err,
              );
            }
          }
          tableRef.current?.appendLocalItem((newId) => {
            draftsRef.current[newId] = { ...draft, computationId: newId };
            return toTableRef({ ...draft, computationId: newId } as UiComputation);
          }, "new");
        }}
      />
    </>
  );
};
