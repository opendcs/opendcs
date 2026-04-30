import type {
  ApiAppRef,
  ApiCompParm,
  ApiComputation,
  ApiDataType,
  ApiSiteRef,
  ApiTsGroupRef,
} from "opendcs-api";

export type UiComputation = Partial<ApiComputation>;

const hasAssignedId = (value: number | undefined): value is number =>
  value !== undefined && value > 0;

const validDataTypeName = (value: string | undefined): string | undefined => {
  const trimmed = value?.trim();
  return trimmed && trimmed.length > 0 ? trimmed : undefined;
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

const normalizeLookupValue = (value: string | undefined): string =>
  value?.trim().toLowerCase() ?? "";

const canonicalDataTypeName = (dataType: ApiDataType): string | undefined => {
  if (dataType.displayName?.trim()) {
    return dataType.displayName.trim();
  }
  if (dataType.standard?.trim() && dataType.code?.trim()) {
    return `${dataType.standard.trim()}:${dataType.code.trim()}`;
  }
  if (dataType.code?.trim()) {
    return dataType.code.trim();
  }
  return undefined;
};

const resolveParmSite = (
  parm: ApiCompParm,
  siteOptions: ApiSiteRef[],
): Pick<ApiCompParm, "siteId" | "siteName"> => {
  const existingSiteId = hasAssignedId(parm.siteId) ? parm.siteId : undefined;
  if (existingSiteId !== undefined) {
    const matched = siteOptions.find((site) => site.siteId === existingSiteId);
    return {
      siteId: existingSiteId,
      siteName: matched?.publicName?.trim() || parm.siteName?.trim() || parm.siteName,
    };
  }

  const siteName = parm.siteName?.trim();
  if (!siteName) {
    return { siteId: undefined, siteName: undefined };
  }

  const normalizedSiteName = normalizeLookupValue(siteName);
  const publicNameMatch = siteOptions.find(
    (site) => normalizeLookupValue(site.publicName ?? undefined) === normalizedSiteName,
  );
  if (hasAssignedId(publicNameMatch?.siteId)) {
    return {
      siteId: publicNameMatch.siteId,
      siteName: publicNameMatch.publicName?.trim() || siteName,
    };
  }

  const aliasMatches = siteOptions.filter((site) =>
    Object.values(site.sitenames ?? {}).some(
      (name) => normalizeLookupValue(name) === normalizedSiteName,
    ),
  );
  if (aliasMatches.length === 1 && hasAssignedId(aliasMatches[0].siteId)) {
    return {
      siteId: aliasMatches[0].siteId,
      siteName: aliasMatches[0].publicName?.trim() || siteName,
    };
  }

  return { siteId: undefined, siteName };
};

const resolveParmDataType = (
  parm: ApiCompParm,
  dataTypeOptions: ApiDataType[],
): Pick<ApiCompParm, "dataType" | "dataTypeId"> => {
  const existingDataTypeId = hasAssignedId(parm.dataTypeId)
    ? parm.dataTypeId
    : undefined;
  if (existingDataTypeId !== undefined) {
    const matched = dataTypeOptions.find(
      (dataType) => dataType.id === existingDataTypeId,
    );
    return {
      dataType: matched
        ? canonicalDataTypeName(matched)
        : validDataTypeName(parm.dataType),
      dataTypeId: existingDataTypeId,
    };
  }

  const dataTypeName = validDataTypeName(parm.dataType);
  if (!dataTypeName) {
    return { dataType: undefined, dataTypeId: undefined };
  }

  const normalizedDataTypeName = normalizeLookupValue(dataTypeName);
  const exactCanonicalMatch = dataTypeOptions.find(
    (dataType) =>
      normalizeLookupValue(canonicalDataTypeName(dataType)) === normalizedDataTypeName,
  );
  if (hasAssignedId(exactCanonicalMatch?.id)) {
    return {
      dataType: canonicalDataTypeName(exactCanonicalMatch),
      dataTypeId: exactCanonicalMatch.id,
    };
  }

  const exactCodeMatches = dataTypeOptions.filter(
    (dataType) => normalizeLookupValue(dataType.code) === normalizedDataTypeName,
  );
  if (exactCodeMatches.length === 1 && hasAssignedId(exactCodeMatches[0].id)) {
    return {
      dataType: canonicalDataTypeName(exactCodeMatches[0]) ?? dataTypeName,
      dataTypeId: exactCodeMatches[0].id,
    };
  }

  return {
    dataType: dataTypeName,
    dataTypeId: undefined,
  };
};

export const resolveComputationParmReferences = (
  computation: ApiComputation,
  siteOptions: ApiSiteRef[],
  dataTypeOptions: ApiDataType[],
): ApiComputation => ({
  ...computation,
  parmList: (computation.parmList ?? []).map((parm) => {
    const site = resolveParmSite(parm, siteOptions);
    const dataType = resolveParmDataType(parm, dataTypeOptions);
    return sanitizeParmForSave({
      ...parm,
      ...site,
      ...dataType,
    });
  }),
});

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
