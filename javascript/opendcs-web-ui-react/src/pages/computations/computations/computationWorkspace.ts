import type {
  ApiAlgoParm,
  ApiCompParm,
  ApiComputation,
  ApiPropSpec,
} from "opendcs-api";
import type { UiState } from "../../../util/Actions";
import type { RowState } from "../../../util/DataTables";

const STORAGE_KEY = "odcs-computation-workspace";

export type UiComputation = Partial<ApiComputation>;

export interface ComputationWorkspaceState {
  drafts: Record<number, UiComputation>;
  rowState: RowState<number>;
  selectionTargetId: number | null;
}

const emptyWorkspaceState = (): ComputationWorkspaceState => ({
  drafts: {},
  rowState: {},
  selectionTargetId: null,
});

const hasWindow = (): boolean => typeof window !== "undefined";

export const loadComputationWorkspace = (): ComputationWorkspaceState => {
  if (!hasWindow()) {
    return emptyWorkspaceState();
  }

  try {
    const raw = window.sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return emptyWorkspaceState();
    }

    const parsed = JSON.parse(raw) as Partial<ComputationWorkspaceState>;
    return {
      drafts: parsed.drafts ?? {},
      rowState: parsed.rowState ?? {},
      selectionTargetId: parsed.selectionTargetId ?? null,
    };
  } catch (error) {
    console.warn("Failed to load computation workspace", error);
    return emptyWorkspaceState();
  }
};

export const saveComputationWorkspace = (
  workspace: ComputationWorkspaceState,
): void => {
  if (!hasWindow()) {
    return;
  }

  window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(workspace));
};

export const getNextComputationDraftId = (
  drafts: Record<number, UiComputation>,
): number => {
  const draftIds = Object.keys(drafts)
    .map((id) => Number.parseInt(id, 10))
    .filter((id) => Number.isFinite(id) && id < 0);

  return draftIds.length === 0 ? -1 : Math.min(...draftIds) - 1;
};

export const mergeAlgorithmParms = (
  existingParms: ApiCompParm[] | undefined,
  algorithmParms: ApiAlgoParm[] | undefined,
): ApiCompParm[] | undefined => {
  if (!algorithmParms || algorithmParms.length === 0) {
    return existingParms;
  }

  const existingByRole = new Map<string, ApiCompParm>();
  (existingParms ?? []).forEach((parm) => {
    const role = (parm.algoRoleName ?? "").trim().toLowerCase();
    if (role.length > 0) {
      existingByRole.set(role, parm);
    }
  });

  const requiredParms = algorithmParms
    .filter((parm) => (parm.roleName ?? "").trim().length > 0)
    .map((parm) => {
      const roleName = parm.roleName?.trim() ?? "";
      const existing = existingByRole.get(roleName.toLowerCase());
      return {
        ...existing,
        algoRoleName: roleName,
        algoParmType: parm.parmType,
      } as ApiCompParm;
    });

  const requiredRoles = new Set(
    requiredParms.map((parm) => (parm.algoRoleName ?? "").trim().toLowerCase()),
  );
  const extras = (existingParms ?? []).filter((parm) => {
    const role = (parm.algoRoleName ?? "").trim().toLowerCase();
    return role.length === 0 || !requiredRoles.has(role);
  });

  return [...requiredParms, ...extras];
};

export const mergeAlgorithmProps = (
  existingProps: Record<string, string> | undefined,
  algorithmProps: Record<string, string> | undefined,
  algorithmPropSpecs: ApiPropSpec[] | undefined,
): Record<string, string> | undefined => {
  const merged: Record<string, string> = {};
  let hasProps = false;

  (algorithmPropSpecs ?? []).forEach((spec) => {
    const name = spec.name?.trim();
    if (name) {
      merged[name] = "";
      hasProps = true;
    }
  });

  Object.entries(algorithmProps ?? {}).forEach(([name, value]) => {
    merged[name] = value;
    hasProps = true;
  });

  Object.entries(existingProps ?? {}).forEach(([name, value]) => {
    merged[name] = value;
    hasProps = true;
  });

  return hasProps ? merged : existingProps;
};

export const applySelectedAlgorithmToWorkspace = (algorithm: {
  algorithmId: number;
  algorithmName: string;
  algorithmDescription?: string;
  algorithmParms?: ApiAlgoParm[];
  algorithmProps?: Record<string, string>;
  algorithmPropSpecs?: ApiPropSpec[];
}): number => {
  const workspace = loadComputationWorkspace();
  const targetId =
    workspace.selectionTargetId ?? getNextComputationDraftId(workspace.drafts);
  const existingDraft = workspace.drafts[targetId] ?? { computationId: targetId };
  const existingComment = existingDraft.comment?.trim();
  const nextMode: UiState =
    workspace.rowState[targetId] ?? (targetId > 0 ? "edit" : "new");

  saveComputationWorkspace({
    drafts: {
      ...workspace.drafts,
      [targetId]: {
        ...existingDraft,
        computationId: targetId,
        algorithmId: algorithm.algorithmId,
        algorithmName: algorithm.algorithmName,
        comment:
          existingComment && existingComment.length > 0
            ? existingDraft.comment
            : (algorithm.algorithmDescription ?? ""),
        props: mergeAlgorithmProps(
          existingDraft.props,
          algorithm.algorithmProps,
          algorithm.algorithmPropSpecs,
        ),
        parmList: mergeAlgorithmParms(existingDraft.parmList, algorithm.algorithmParms),
      },
    },
    rowState: {
      ...workspace.rowState,
      [targetId]: nextMode,
    },
    selectionTargetId: null,
  });

  return targetId;
};
