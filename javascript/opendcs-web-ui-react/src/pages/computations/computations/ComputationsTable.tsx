import { useCallback, useMemo } from "react";
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

  const descriptionSnippet = useCallback((description: string | undefined): string => {
    const normalized = (description ?? "").replace(/\s+/g, " ").trim();
    if (normalized.length <= 120) return normalized;
    return `${normalized.slice(0, 117).trimEnd()}...`;
  }, []);

  const renderDescription = useCallback(
    (data: unknown, type: string) => {
      const d = typeof data === "string" ? data : "";
      return type !== "display" ? d : descriptionSnippet(d);
    },
    [descriptionSnippet],
  );

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
        key: "edit",
        icon: "bi-pencil",
        variant: "warning",
        aria: (row) => t("computations:editor.edit_for", { id: row.computationId }),
        onClick: ({ row, api }) => api.setMode(row, "edit"),
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
    [t, actions],
  );

  return (
    <AppDataTable<TableComputationRef, number, ApiComputation>
      data={computations}
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
            : Promise.resolve({ computationId } as UiComputation);

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
  );
};
