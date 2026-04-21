import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import { useTranslation } from "react-i18next";
import { Suspense, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { dtLangs } from "../../../lang";
import type {
  ApiAlgorithm,
  ApiAppRef,
  ApiComputation,
  ApiComputationRef,
  ApiTsGroupRef,
} from "opendcs-api";
import Computation, { ComputationSkeleton, type UiComputation } from "./Computation";
import type { UiState } from "../../../util/Actions";
import { useContextWrapper } from "../../../util/ContextWrapper";
import type { RowState } from "../../../util/DataTables";
import { useNavigate } from "react-router-dom";
import {
  getNextComputationDraftId,
  loadComputationWorkspace,
  saveComputationWorkspace,
} from "./computationWorkspace";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export type TableComputationRef = Partial<ApiComputationRef>;

export interface ComputationsTableProperties {
  computations: TableComputationRef[];
  getComputation?: (computationId: number) => Promise<ApiComputation>;
  getAlgorithm?: (algorithmId: number) => Promise<ApiAlgorithm>;
  actions?: {
    save?: (item: ApiComputation) => void | Promise<ApiComputation | void>;
    remove?: (item: number) => void | Promise<void>;
    run?: (item: number, start: Date, end: Date) => void | Promise<void>;
  };
  processOptions?: ApiAppRef[];
  groupOptions?: ApiTsGroupRef[];
}

const isOutputParm = (parm: { algoParmType?: string }): boolean =>
  (parm.algoParmType ?? "").trim().toLowerCase().startsWith("o");

const isOutputProp = (propName: string): boolean =>
  propName.trim().toLowerCase().startsWith("output");

const descriptionSnippet = (value: unknown): string => {
  const description = String(value ?? "").trim();
  const maxLength = 80;
  if (description.length <= maxLength) {
    return description;
  }
  return `${description.slice(0, maxLength - 1).trimEnd()}...`;
};

export const ComputationsTable: React.FC<ComputationsTableProperties> = ({
  computations,
  getComputation,
  getAlgorithm,
  actions = {},
  processOptions = [],
  groupOptions = [],
}) => {
  const { toDom } = useContextWrapper();
  const navigate = useNavigate();
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["computations", "translation"]);
  const saveAction = actions.save;
  const removeAction = actions.remove;
  const runAction = actions.run;
  const [workspace, setWorkspace] = useState(loadComputationWorkspace);
  const [removingIds, setRemovingIds] = useState<number[]>([]);
  const workspaceRef = useRef(workspace);
  const rowStateRef = useRef(workspace.rowState);
  const draftsRef = useRef(workspace.drafts);
  const prevDataIdsRef = useRef("");
  const renderComputationRef = useRef<
    (data: TableComputationRef, edit: boolean) => Node
  >(() => document.createElement("div"));
  const childModeRef = useRef<RowState<number>>({});
  const childNodesRef = useRef<Record<number, Node>>({});
  const closedDraftIdsRef = useRef<Set<number>>(new Set());

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    Object.keys(window.localStorage)
      .filter((key) => key.startsWith("DataTables_computationTable_"))
      .forEach((key) => window.localStorage.removeItem(key));
  }, []);

  useEffect(() => {
    workspaceRef.current = workspace;
    rowStateRef.current = workspace.rowState;
    draftsRef.current = workspace.drafts;
  }, [workspace]);

  useEffect(() => {
    saveComputationWorkspace(workspace);
  }, [workspace]);

  useEffect(() => {
    setRemovingIds((current) =>
      current.filter((id) =>
        computations.some((computation) => computation.computationId === id),
      ),
    );
  }, [computations]);

  const toTableRef = useCallback((comp: UiComputation): TableComputationRef => {
    return {
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
    };
  }, []);

  const ensureDraftVisible = useCallback(() => {
    const dt = table.current?.dt();
    if (!dt) {
      return;
    }

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
  }, []);

  const updateDraft = useCallback((draft: UiComputation) => {
    const computationId = draft.computationId;
    if (computationId === undefined) {
      return;
    }

    setWorkspace((current) => ({
      ...current,
      drafts: {
        ...current.drafts,
        [computationId]: {
          ...(current.drafts[computationId] ?? {}),
          ...draft,
        },
      },
    }));
  }, []);

  const persistDraft = useCallback((draft: UiComputation) => {
    const computationId = draft.computationId;
    if (computationId === undefined) {
      return;
    }
    if (closedDraftIdsRef.current.has(computationId)) {
      return;
    }

    const nextDrafts = {
      ...draftsRef.current,
      [computationId]: {
        ...(draftsRef.current[computationId] ?? {}),
        ...draft,
      },
    };
    draftsRef.current = nextDrafts;
    saveComputationWorkspace({
      ...workspaceRef.current,
      drafts: nextDrafts,
      rowState: rowStateRef.current,
    });
  }, []);

  const clearDraft = useCallback((computationId: number) => {
    closedDraftIdsRef.current.add(computationId);
    const { [computationId]: _removed, ...remainingDraftsRef } = draftsRef.current;
    draftsRef.current = remainingDraftsRef;
    setWorkspace((current) => {
      const { [computationId]: _, ...remainingDrafts } = current.drafts;
      return {
        ...current,
        drafts: remainingDrafts,
      };
    });
  }, []);

  const setRowMode = useCallback((computationId: number, mode: UiState) => {
    if (mode === "edit" || mode === "new") {
      closedDraftIdsRef.current.delete(computationId);
    }
    setWorkspace((current) => {
      if (mode === undefined) {
        const { [computationId]: _, ...remainingRows } = current.rowState;
        return {
          ...current,
          rowState: remainingRows,
        };
      }

      return {
        ...current,
        rowState: {
          ...current.rowState,
          [computationId]: mode,
        },
      };
    });
  }, []);

  const createDraft = useCallback(
    (seed: UiComputation = {}) => {
      const draftId =
        seed.computationId !== undefined
          ? seed.computationId
          : getNextComputationDraftId(draftsRef.current);
      const draft = {
        ...(draftsRef.current[draftId] ?? {}),
        ...seed,
        computationId: draftId,
      };

      closedDraftIdsRef.current.delete(draftId);
      updateDraft(draft);
      return draft;
    },
    [updateDraft],
  );

  const copiedComputation = useCallback(
    (source: ApiComputation, newId: number): UiComputation => {
      const copiedProps = Object.fromEntries(
        Object.entries(source.props ?? {}).filter(([key]) => !isOutputProp(key)),
      );
      const copiedParmList = (source.parmList ?? [])
        .filter((parm) => !isOutputParm(parm))
        .map((parm) => ({ ...parm }));
      return {
        ...source,
        computationId: newId,
        name: "",
        props: copiedProps,
        parmList: copiedParmList,
      };
    },
    [],
  );

  const computationData = useMemo(() => {
    const hiddenIds = new Set(removingIds);
    const localDraftRows = Object.values(workspace.drafts)
      .filter(
        (draft): draft is UiComputation =>
          draft.computationId !== undefined && draft.computationId < 0,
      )
      .map((draft) => toTableRef(draft));

    return [
      ...computations.filter(
        (computation) => !hiddenIds.has(computation.computationId ?? Number.NaN),
      ),
      ...localDraftRows,
    ];
  }, [computations, workspace.drafts, toTableRef, removingIds]);

  const renderEnabled = useCallback((data: unknown, type: string) => {
    const enabled = Boolean(data);
    if (type !== "display") {
      return enabled ? 1 : 0;
    }
    return enabled ? "✓" : "";
  }, []);

  const columns = useMemo(
    () => [
      {
        data: "computationId",
        defaultContent: "new",
        className: "dt-left",
        render: (data: unknown, type: string) => {
          const id = typeof data === "number" ? data : Number(data);
          if (!Number.isFinite(id)) {
            return type === "display" ? "new" : data;
          }
          if (type === "display" && id < 0) {
            return "new";
          }
          return id;
        },
      },
      { data: "name", defaultContent: "" },
      { data: "algorithmName", defaultContent: "" },
      { data: "processName", defaultContent: "" },
      {
        data: "enabled",
        defaultContent: "",
        className: "dt-center",
        render: renderEnabled,
      },
      {
        data: "description",
        defaultContent: "",
        render: (data: unknown, type: string) =>
          type === "display" ? descriptionSnippet(data) : (data ?? ""),
      },
      {
        data: null,
        name: "actions",
        orderable: false,
        searchable: false,
        defaultContent: "",
      },
    ],
    [renderEnabled],
  );

  const options: DataTableProps["options"] = {
    paging: true,
    responsive: true,
    stateSave: false,
    destroy: true,
    language: dtLangs.get(i18n.language),
    createdRow: (_row, _data, dataIndex) => {
      table.current?.dt()?.row(dataIndex).node().classList.add("child-toggle");
    },
    drawCallback: function () {
      const dt = this.api();
      const actionsColumnIndex = dt.column("actions:name").index("visible");
      dt.rows({ page: "current", search: "applied" }).every(function () {
        const data = this.data() as TableComputationRef | undefined;
        if (!data) return;
        const idx = data.computationId!;
        const node = this.node() as HTMLTableRowElement;
        const actionsCell =
          typeof actionsColumnIndex === "number"
            ? node.children.item(actionsColumnIndex)
            : null;
        if (!actionsCell) return;

        const inEdit =
          rowStateRef.current[idx] === "edit" || rowStateRef.current[idx] === "new";
        const editLabel = t("computations:editor.edit_for", { id: idx });
        const copyLabel = t("computations:editor.copy_for", { id: idx });
        const deleteLabel = t("computations:editor.delete_for", { id: idx });
        const editBtn = !inEdit
          ? `<button type="button" class="btn btn-warning btn-sm dt-action-edit" data-id="${idx}" aria-label="${editLabel}"><i class="bi bi-pencil"></i></button>`
          : "";
        const deleteBtn =
          idx !== undefined
            ? ` <button type="button" class="btn btn-danger btn-sm dt-action-delete" data-id="${idx}" aria-label="${deleteLabel}"><i class="bi bi-trash"></i></button>`
            : "";
        const copyBtn =
          idx !== undefined
            ? ` <button type="button" class="btn btn-info btn-sm dt-action-copy" data-id="${idx}" aria-label="${copyLabel}"><i class="bi bi-files"></i></button>`
            : "";
        (actionsCell as HTMLElement).style.whiteSpace = "nowrap";
        actionsCell.innerHTML = editBtn + deleteBtn + copyBtn;

        const state = rowStateRef.current[idx];
        const isShown = this.child.isShown();
        const prevMode = childModeRef.current[idx];
        if (state !== undefined) {
          if (!isShown || prevMode !== state) {
            if (isShown) {
              this.child.hide();
            }
            if (prevMode !== state) {
              const edit = state !== "show";
              const childNode = renderComputationRef.current(data, edit);
              childNodesRef.current[idx] = childNode;
              this.child(childNode, "child-row").show();
            } else {
              const cached = childNodesRef.current[idx];
              if (cached) {
                this.child(cached, "child-row").show();
              } else {
                const edit = state !== "show";
                const childNode = renderComputationRef.current(data, edit);
                childNodesRef.current[idx] = childNode;
                this.child(childNode, "child-row").show();
              }
            }
            childModeRef.current[idx] = state;
          }
        } else if (isShown) {
          this.child(false);
          delete childModeRef.current[idx];
          delete childNodesRef.current[idx];
        }
      });
    },
    layout: {
      top1Start: {
        buttons: [
          {
            text: "+",
            action: () => {
              ensureDraftVisible();
              const draft = createDraft();
              if (draft.computationId !== undefined) {
                setRowMode(draft.computationId, "new");
              }
            },
            attr: { "aria-label": t("computations:add_computation") },
          },
        ],
      },
    },
  };

  const renderComputation = useCallback(
    (data: TableComputationRef, edit: boolean = false): Node => {
      const computationId = data.computationId!;
      const storedDraft = draftsRef.current[computationId];
      const computationPromise: Promise<UiComputation> =
        storedDraft !== undefined
          ? Promise.resolve(storedDraft)
          : computationId > 0
            ? getComputation!(computationId)
            : Promise.resolve({ computationId } as UiComputation);

      const algorithmPromise: Promise<ApiAlgorithm | undefined> =
        computationPromise.then(async (comp) => {
          if (!getAlgorithm || !comp.algorithmId || comp.algorithmId <= 0) {
            return undefined;
          }
          try {
            return await getAlgorithm(comp.algorithmId);
          } catch (error) {
            console.warn(
              `Failed to load algorithm for algorithmId=${comp.algorithmId}`,
              error,
            );
            return undefined;
          }
        });

      const node = toDom(
        <Suspense fallback={<ComputationSkeleton edit={edit} />}>
          <Computation
            computation={computationPromise}
            algorithm={algorithmPromise}
            onDraftChange={(draft) => {
              persistDraft(draft);
            }}
            onSelectAlgorithm={(draft) => {
              if (draft.computationId === undefined) {
                return;
              }
              const nextWorkspace = {
                ...workspace,
                drafts: {
                  ...workspace.drafts,
                  [draft.computationId]: {
                    ...(workspace.drafts[draft.computationId] ?? {}),
                    ...draft,
                  },
                },
                selectionTargetId: draft.computationId,
              };
              setWorkspace(nextWorkspace);
              saveComputationWorkspace(nextWorkspace);
              navigate("/algorithms");
            }}
            actions={{
              save: async (comp: ApiComputation) => {
                await Promise.resolve(saveAction?.(comp));
                delete childModeRef.current[computationId];
                delete childNodesRef.current[computationId];
                clearDraft(computationId);
                setRowMode(computationId, undefined);
                setWorkspace((current) => ({
                  ...current,
                  selectionTargetId: null,
                }));
              },
              run: runAction,
              cancel: (item) => {
                delete childModeRef.current[item];
                delete childNodesRef.current[item];
                clearDraft(item);
                setRowMode(item, undefined);
                setWorkspace((current) => ({
                  ...current,
                  selectionTargetId: null,
                }));
              },
            }}
            edit={edit}
            processOptions={processOptions}
            groupOptions={groupOptions}
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
    [
      getComputation,
      getAlgorithm,
      toDom,
      workspace,
      persistDraft,
      navigate,
      saveAction,
      runAction,
      clearDraft,
      setRowMode,
      processOptions,
      groupOptions,
    ],
  );

  useEffect(() => {
    renderComputationRef.current = renderComputation;
  }, [renderComputation]);

  useEffect(() => {
    childModeRef.current = {};
    childNodesRef.current = {};
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

        const editBtn = target.closest(".dt-action-edit");
        if (editBtn) {
          e.preventDefault();
          e.stopPropagation();
          const id = Number(editBtn.getAttribute("data-id"));
          setRowMode(id, "edit");
          return;
        }

        const copyBtn = target.closest(".dt-action-copy");
        if (copyBtn) {
          e.preventDefault();
          e.stopPropagation();
          const id = Number(copyBtn.getAttribute("data-id"));
          const sourcePromise =
            id > 0 && getComputation
              ? getComputation(id)
              : Promise.resolve(draftsRef.current[id] as ApiComputation | undefined);
          sourcePromise
            .then((source) => {
              if (!source) {
                return;
              }
              ensureDraftVisible();
              const draft = createDraft(
                copiedComputation(source, getNextComputationDraftId(draftsRef.current)),
              );
              if (draft.computationId !== undefined) {
                setRowMode(draft.computationId, "new");
              }
            })
            .catch((error) => {
              console.warn(`Failed to copy computation ${id}`, error);
            });
          return;
        }

        const deleteBtn = target.closest(".dt-action-delete");
        if (deleteBtn) {
          e.preventDefault();
          e.stopPropagation();
          const id = Number(deleteBtn.getAttribute("data-id"));
          if (!removeAction) {
            if (id < 0) {
              delete childModeRef.current[id];
              delete childNodesRef.current[id];
              clearDraft(id);
              setRowMode(id, undefined);
            }
            return;
          }
          delete childModeRef.current[id];
          delete childNodesRef.current[id];
          clearDraft(id);
          setRowMode(id, undefined);
          setWorkspace((current) => ({
            ...current,
            selectionTargetId:
              current.selectionTargetId === id ? null : current.selectionTargetId,
          }));
          if (id > 0) {
            setRemovingIds((current) =>
              current.includes(id) ? current : [...current, id],
            );
            Promise.resolve(removeAction(id)).catch((error) => {
              console.error(`Failed to remove computation ${id}`, error);
              setRemovingIds((current) => current.filter((value) => value !== id));
            });
          }
          return;
        }

        e.preventDefault();
        e.stopPropagation();
        const dt = table.current!.dt()!;
        const row = dt.row(tr as HTMLTableRowElement);
        const idx = (row.data() as TableComputationRef).computationId!;
        const existing = rowStateRef.current[idx];
        setRowMode(idx, existing === undefined ? "show" : undefined);
      });
  }, [
    i18n.language,
    getComputation,
    ensureDraftVisible,
    clearDraft,
    createDraft,
    copiedComputation,
    setRowMode,
    removeAction,
  ]);

  useEffect(() => {
    if (table.current?.dt()) {
      const currentIds = computationData
        .map((row) => String(row.computationId ?? ""))
        .join(",");
      const idsChanged = currentIds !== prevDataIdsRef.current;
      prevDataIdsRef.current = currentIds;
      const hasActiveEditor = Object.values(rowStateRef.current).some(
        (mode) => mode === "edit" || mode === "new",
      );
      if (!idsChanged && hasActiveEditor) {
        return;
      }
      const dt = table.current.dt()!;
      dt.rows({ page: "current", search: "applied" }).invalidate();
      dt.draw(false);
    }
  }, [computationData]);

  useEffect(() => {
    table.current?.dt()?.draw(false);
  }, [workspace.rowState]);

  return (
    <DataTable
      key={i18n.language}
      id="computationTable"
      columns={columns}
      data={computationData}
      options={options}
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
