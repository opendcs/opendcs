import type {
  ApiAppRef,
  ApiCompParm,
  ApiComputation,
  ApiTsGroupRef,
} from "opendcs-api";

export type UiComputation = Partial<ApiComputation>;

const hasAssignedId = (value: number | undefined): value is number =>
  value !== undefined && value > 0;

const validDataTypeName = (value: string | undefined): string | undefined => {
  const trimmed = value?.trim();
  if (!trimmed) {
    return undefined;
  }

  // The REST mapper currently expects a standard "source:code" data type name.
  return trimmed.includes(":") ? trimmed : undefined;
};

const sanitizeParmForSave = (parm: ApiCompParm): ApiCompParm => {
  const dataType = validDataTypeName(parm.dataType);

  return {
    ...parm,
    tsKey: hasAssignedId(parm.tsKey) ? parm.tsKey : undefined,
    dataType,
    dataTypeId:
      dataType && hasAssignedId(parm.dataTypeId) ? parm.dataTypeId : undefined,
    siteId: hasAssignedId(parm.siteId) ? parm.siteId : undefined,
  };
};

const sanitizePropsForSave = (
  props: Record<string, string> | undefined,
): Record<string, string> =>
  Object.fromEntries(
    Object.entries(props ?? {}).filter(([name]) => name.trim().length > 0),
  );

const findProcessOption = (
  comp: UiComputation,
  processOptions: ApiAppRef[],
): ApiAppRef | undefined => {
  if (hasAssignedId(comp.appId)) {
    const matchedById = processOptions.find((app) => app.appId === comp.appId);
    if (matchedById) {
      return matchedById;
    }
  }
  if (!comp.applicationName) {
    return (
      processOptions.find(
        (app) => app.appName === "compproc" && hasAssignedId(app.appId),
      ) ??
      processOptions.find(
        (app) => app.appType === "ComputationProcess" && hasAssignedId(app.appId),
      ) ??
      processOptions.find((app) => hasAssignedId(app.appId))
    );
  }
  return processOptions.find((app) => app.appName === comp.applicationName);
};

const findGroupOption = (
  comp: UiComputation,
  groupOptions: ApiTsGroupRef[],
): ApiTsGroupRef | undefined => {
  if (hasAssignedId(comp.groupId)) {
    const matchedById = groupOptions.find((group) => group.groupId === comp.groupId);
    if (matchedById) {
      return matchedById;
    }
  }
  if (!comp.groupName) {
    return undefined;
  }
  return groupOptions.find((group) => group.groupName === comp.groupName);
};

export const normalizeComputationForSave = (
  comp: UiComputation,
  processOptions: ApiAppRef[],
  groupOptions: ApiTsGroupRef[],
): UiComputation => {
  const processOption = findProcessOption(comp, processOptions);
  const groupOption = findGroupOption(comp, groupOptions);

  return {
    ...comp,
    appId: processOption?.appId,
    applicationName: processOption?.appName ?? comp.applicationName ?? "",
    groupId: groupOption?.groupId,
    groupName: groupOption?.groupName ?? comp.groupName ?? "",
  };
};

export const saveComputationDraft = async (
  comp: UiComputation,
  localParms: ApiCompParm[],
  processOptions: ApiAppRef[],
  groupOptions: ApiTsGroupRef[],
  save?: (item: ApiComputation) => void | Promise<ApiComputation | void>,
) => {
  const normalized = normalizeComputationForSave(comp, processOptions, groupOptions);
  if (!normalized.name?.trim()) {
    throw new Error("Enter a computation name before saving.");
  }
  if (!hasAssignedId(normalized.algorithmId) && !normalized.algorithmName?.trim()) {
    throw new Error("Select an algorithm before saving.");
  }

  return save?.({
    ...(normalized as ApiComputation),
    computationId: hasAssignedId(normalized.computationId)
      ? normalized.computationId
      : undefined,
    algorithmId: hasAssignedId(normalized.algorithmId)
      ? normalized.algorithmId
      : undefined,
    appId: hasAssignedId(normalized.appId) ? normalized.appId : undefined,
    groupId: hasAssignedId(normalized.groupId) ? normalized.groupId : undefined,
    lastModified: undefined,
    props: sanitizePropsForSave(normalized.props),
    parmList: localParms.map(sanitizeParmForSave),
  });
};

export const toSelectValue = (
  id: number | undefined,
  name: string | undefined,
  options: Array<
    { appId?: number; appName?: string } | { groupId?: number; groupName?: string }
  >,
): string => {
  if (hasAssignedId(id)) {
    return String(id);
  }
  if (!name) {
    return "";
  }

  for (const option of options) {
    if ("appName" in option && option.appName === name && hasAssignedId(option.appId)) {
      return String(option.appId);
    }
    if (
      "groupName" in option &&
      option.groupName === name &&
      hasAssignedId(option.groupId)
    ) {
      return String(option.groupId);
    }
  }

  return "";
};
