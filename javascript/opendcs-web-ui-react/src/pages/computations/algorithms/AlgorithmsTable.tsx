import { useMemo, useRef, useState } from "react";
import { Alert } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import { HttpMethod } from "opendcs-api";
import { useTranslation } from "react-i18next";
import type { ApiAlgorithm, ApiAlgorithmRef, ApiPropSpec } from "opendcs-api";
import Algorithm, { AlgorithmSkeleton, type UiAlgorithm } from "./Algorithm";
import type { AlgoParm } from "./AlgorithmParamsTable";
import { CheckForNewModal } from "./CheckForNewModal";
import { useApi } from "../../../contexts/app/ApiContext";
import { algorithmKeys } from "../../../queries/keys";
import type { RemoveAction, SaveAction } from "../../../util/Actions";
import {
  AppDataTable,
  idColumn,
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
  const [syncMessage, setSyncMessage] = useState<string>();
  const { conf, org } = useApi();
  const queryClient = useQueryClient();
  const tableRef = useRef<AppDataTableHandle<TableAlgorithmRef>>(null);
  const draftsRef = useRef<Record<number, UiAlgorithm>>({});

  const syncInstalled = async () => {
    try {
      const context = conf.baseServer.makeRequestContext(
        "/algorithmupdate",
        HttpMethod.POST,
      );
      const response = await fetch(context.getUrl(), {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", "X-ORGANIZATION-ID": org },
        body: JSON.stringify(
          algorithms
            .map((algorithm) => algorithm.algorithmId)
            .filter((id): id is number => id !== undefined && id > 0),
        ),
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const summaries = (await response.json()) as Array<{
        renamedParameters: number;
        renamedProperties: number;
        updatedTypes: number;
        addedAlgorithmParameters: number;
        removedAlgorithmParameters: number;
        addedAlgorithmProperties: number;
        needsReview: number;
      }>;
      const changed = summaries.filter(
        (summary) =>
          summary.renamedParameters +
            summary.renamedProperties +
            summary.addedAlgorithmParameters +
            summary.addedAlgorithmProperties +
            summary.removedAlgorithmParameters +
            summary.updatedTypes >
          0,
      ).length;
      const review = summaries.reduce((sum, summary) => sum + summary.needsReview, 0);
      setSyncMessage(
        `${changed} algorithms updated. ${review} parameter or property conflicts need review.`,
      );
      await queryClient.invalidateQueries({ queryKey: algorithmKeys.all(org) });
    } catch (error) {
      setSyncMessage(`Algorithm update failed: ${String(error)}`);
    }
  };

  const columns = useMemo<ColumnDef<TableAlgorithmRef>[]>(
    () => [
      idColumn("algorithmId", t("algorithms:header.Id")),
      {
        data: "algorithmName",
        header: t("algorithms:header.Name"),
        type: "string",
        defaultSort: "asc",
      },
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
        confirm: true,
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
      {syncMessage && (
        <Alert variant="info" dismissible onClose={() => setSyncMessage(undefined)}>
          {syncMessage}
        </Alert>
      )}
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
            text: "Sync installed algorithms",
            ariaLabel: "Sync installed algorithms",
            onClick: syncInstalled,
          },
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
