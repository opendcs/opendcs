import type { ApiAppRef, ApiComputation, ApiTsGroupRef } from "opendcs-api";

export type UiComputation = Partial<ApiComputation>;

const hasAssignedId = (value: number | undefined): value is number =>
  value !== undefined && value > 0;

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
    return undefined;
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
