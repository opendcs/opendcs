import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  FormGroup,
  Placeholder,
  Row,
  Table,
} from "react-bootstrap";
import { PropertiesTable, type Property } from "../../../components/properties";
import { use, useCallback, useEffect, useMemo, useReducer, useState } from "react";
import type {
  ApiAlgorithm,
  ApiAppRef,
  ApiCompParm,
  ApiComputation,
  ApiTsGroupRef,
} from "opendcs-api";
import { useTranslation } from "react-i18next";
import type { CancelAction, CollectionActions } from "../../../util/Actions";
import { PlayFill, Save, X } from "react-bootstrap-icons";
import { ComputationReducer } from "./ComputationReducer";
import { ComputationParamsTable } from "./ComputationParamsTable";
import { saveComputationDraft, toSelectValue } from "./computationSave";
import type { ComputationRunResult } from "./computationRun";

export type UiComputation = Partial<ApiComputation>;

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };

export const ComputationSkeleton: React.FC<{ edit?: boolean }> = ({ edit = false }) => (
  <Card>
    <Card.Body>
      <Row>
        <Col>
          <Row className="mb-3 align-items-center">
            <Placeholder as={Col} sm={3} animation="glow">
              <Placeholder xs={6} className="rounded" style={LABEL_H} />
            </Placeholder>
            <Placeholder as={Col} sm={9} animation="glow">
              <Placeholder xs={12} className="rounded" style={INPUT_H} />
            </Placeholder>
          </Row>
          <Row className="mb-3 align-items-center">
            <Placeholder as={Col} sm={3} animation="glow">
              <Placeholder xs={9} className="rounded" style={LABEL_H} />
            </Placeholder>
            <Placeholder as={Col} sm={9} animation="glow">
              <Placeholder xs={12} className="rounded" style={INPUT_H} />
            </Placeholder>
          </Row>
          <Row className="mb-3 align-items-center">
            <Placeholder as={Col} sm={3} animation="glow">
              <Placeholder xs={9} className="rounded" style={LABEL_H} />
            </Placeholder>
            <Placeholder as={Col} sm={9} animation="glow">
              <Placeholder xs={12} className="rounded" style={INPUT_H} />
            </Placeholder>
          </Row>
          <Row className="mb-3 align-items-center">
            <Placeholder as={Col} sm={3} animation="glow">
              <Placeholder xs={8} className="rounded" style={LABEL_H} />
            </Placeholder>
            <Placeholder as={Col} sm={9} animation="glow">
              <Placeholder xs={12} className="rounded" style={INPUT_H} />
            </Placeholder>
          </Row>
        </Col>
        <Col>
          <Row className="mb-3 align-items-start">
            <Placeholder as={Col} sm={3} animation="glow">
              <Placeholder xs={10} className="rounded" style={LABEL_H} />
            </Placeholder>
            <Placeholder as={Col} sm={9} animation="glow">
              <Placeholder xs={12} className="rounded" style={{ height: "6rem" }} />
            </Placeholder>
          </Row>
          <Placeholder animation="glow">
            <Placeholder xs={12} className="rounded" style={{ height: "8rem" }} />
          </Placeholder>
        </Col>
      </Row>

      {edit && (
        <Row className="mt-3">
          <Col className="d-flex justify-content-end gap-2">
            <Placeholder animation="glow">
              <Placeholder
                className="rounded"
                style={{ ...INPUT_H, width: "5.5rem" }}
              />
            </Placeholder>
            <Placeholder animation="glow">
              <Placeholder
                className="rounded"
                style={{ ...INPUT_H, width: "4.5rem" }}
              />
            </Placeholder>
          </Col>
        </Row>
      )}
    </Card.Body>
  </Card>
);

export interface ComputationProperties {
  computation: Promise<UiComputation> | UiComputation;
  algorithm?: Promise<ApiAlgorithm | undefined> | ApiAlgorithm | undefined;
  actions?: {
    save?: (item: ApiComputation) => void | Promise<ApiComputation | void>;
    run?: (
      item: number,
      start: Date,
      end: Date,
    ) => void | ComputationRunResult | Promise<void | ComputationRunResult>;
  } & CancelAction<number>;
  edit?: boolean;
  processOptions?: ApiAppRef[];
  groupOptions?: ApiTsGroupRef[];
  onDraftChange?: (draft: UiComputation) => void;
  onSelectAlgorithm?: (draft: UiComputation) => void;
}

const roleKey = (roleName: string | undefined): string =>
  (roleName ?? "").trim().toLowerCase();

const toDateTimeLocalValue = (date: Date): string => {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 16);
};

const snapshotComputationDraft = (
  computation: UiComputation,
  parms: ApiCompParm[],
): string => {
  const props = Object.fromEntries(
    Object.entries(computation.props ?? {}).sort(([left], [right]) =>
      left.localeCompare(right),
    ),
  );
  const normalizedParms = parms.map((parm) => ({
    algoRoleName: parm.algoRoleName ?? "",
    algoParmType: parm.algoParmType ?? "",
    siteId: parm.siteId ?? undefined,
    siteName: parm.siteName ?? "",
    dataTypeId: parm.dataTypeId ?? undefined,
    dataType: parm.dataType ?? "",
    interval: parm.interval ?? "",
    paramType: parm.paramType ?? "",
    duration: parm.duration ?? "",
    version: parm.version ?? "",
    deltaT: parm.deltaT ?? 0,
    deltaTUnits: parm.deltaTUnits ?? "",
    unitsAbbr: parm.unitsAbbr ?? "",
    ifMissing: parm.ifMissing ?? "",
    tsKey: parm.tsKey ?? undefined,
  }));

  return JSON.stringify({
    computationId: computation.computationId ?? undefined,
    name: computation.name ?? "",
    algorithmId: computation.algorithmId ?? undefined,
    algorithmName: computation.algorithmName ?? "",
    appId: computation.appId ?? undefined,
    applicationName: computation.applicationName ?? "",
    groupId: computation.groupId ?? undefined,
    groupName: computation.groupName ?? "",
    enabled: computation.enabled ?? false,
    comment: computation.comment ?? "",
    props,
    parmList: normalizedParms,
  });
};

const mergeRequiredParms = (
  existingParms: ApiCompParm[],
  requiredParms: ApiCompParm[],
): ApiCompParm[] => {
  const existingByRole = new Map<string, ApiCompParm>();
  existingParms.forEach((parm) => existingByRole.set(roleKey(parm.algoRoleName), parm));

  const requiredMerged = requiredParms
    .filter((parm) => roleKey(parm.algoRoleName) !== "")
    .map((required) => {
      const key = roleKey(required.algoRoleName);
      const existing = existingByRole.get(key);
      if (existing) {
        return {
          ...existing,
          algoRoleName: existing.algoRoleName ?? required.algoRoleName,
          algoParmType: existing.algoParmType ?? required.algoParmType,
        };
      }
      return {
        algoRoleName: required.algoRoleName,
        algoParmType: required.algoParmType,
      } as ApiCompParm;
    });

  const requiredKeys = new Set(
    requiredMerged.map((parm) => roleKey(parm.algoRoleName)),
  );
  const extras = existingParms.filter(
    (parm) => !requiredKeys.has(roleKey(parm.algoRoleName)),
  );

  return [...requiredMerged, ...extras];
};

const requiredParmsFromAlgorithm = (algorithm?: ApiAlgorithm): ApiCompParm[] =>
  (algorithm?.parms ?? [])
    .filter((parm) => (parm.roleName ?? "").trim().length > 0)
    .map((parm) => ({
      algoRoleName: parm.roleName,
      algoParmType: parm.parmType,
    }));

export const Computation: React.FC<ComputationProperties> = ({
  computation,
  algorithm,
  actions = {},
  edit = false,
  processOptions = [],
  groupOptions = [],
  onDraftChange,
  onSelectAlgorithm,
}) => {
  const { t } = useTranslation(["computations", "translation"]);
  const saveAction = actions.save;
  const runAction = actions.run;
  const cancelAction = actions.cancel;
  const providedComputation =
    computation instanceof Promise ? use(computation) : computation;
  const providedAlgorithm = algorithm instanceof Promise ? use(algorithm) : algorithm;
  const initialParms = mergeRequiredParms(
    providedComputation.parmList ?? [],
    requiredParmsFromAlgorithm(providedAlgorithm),
  );
  const [localComputation, dispatch] = useReducer(
    ComputationReducer,
    providedComputation,
  );
  const [localParms, setLocalParms] = useState<ApiCompParm[]>(() => initialParms);
  const [savedSnapshot, setSavedSnapshot] = useState(() =>
    snapshotComputationDraft(providedComputation, initialParms),
  );
  const defaultRunEnd = useMemo(() => new Date(), []);
  const defaultRunStart = useMemo(
    () => new Date(defaultRunEnd.getTime() - 24 * 60 * 60 * 1000),
    [defaultRunEnd],
  );
  const [runStart, setRunStart] = useState(toDateTimeLocalValue(defaultRunStart));
  const [runEnd, setRunEnd] = useState(toDateTimeLocalValue(defaultRunEnd));
  const [running, setRunning] = useState(false);
  const [runError, setRunError] = useState<string | null>(null);
  const [runSucceeded, setRunSucceeded] = useState(false);
  const [runResult, setRunResult] = useState<ComputationRunResult | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const activeComputationId =
    localComputation.computationId ?? providedComputation.computationId;
  const canShowRunControls = runAction !== undefined;
  const canRunComputation =
    activeComputationId !== undefined && activeComputationId > 0 && canShowRunControls;
  const hasUnsavedChanges = useMemo(
    () => snapshotComputationDraft(localComputation, localParms) !== savedSnapshot,
    [localComputation, localParms, savedSnapshot],
  );

  useEffect(() => {
    if (!hasUnsavedChanges) {
      return;
    }
    setRunSucceeded(false);
    setRunError(null);
    setRunResult(null);
  }, [hasUnsavedChanges]);

  const props = useMemo<Property[]>(() => {
    const saved = localComputation.props || {};
    return Object.entries(saved).map(([name, value]) => ({ name, value }));
  }, [localComputation.props]);

  const propertyActions: CollectionActions<Property, string> = edit
    ? {
        remove: (propName) => {
          dispatch({ type: "delete_prop", payload: { name: propName } });
        },
        save: (prop) => {
          dispatch({
            type: "save_prop",
            payload: { name: prop.name, value: prop.value },
          });
        },
      }
    : {};

  const saveComputation = useCallback(
    async (comp: UiComputation) => {
      setSaving(true);
      setSaveError(null);
      try {
        const saved = await saveComputationDraft(
          comp,
          localParms,
          processOptions,
          groupOptions,
          saveAction,
        );
        const savedComputation =
          saved ??
          ({
            ...(comp as ApiComputation),
            parmList: localParms,
          } as ApiComputation);
        const mergedSavedParms = mergeRequiredParms(
          savedComputation.parmList ?? [],
          requiredParmsFromAlgorithm(providedAlgorithm),
        );
        dispatch({ type: "replace", payload: savedComputation });
        setLocalParms(mergedSavedParms);
        setSavedSnapshot(snapshotComputationDraft(savedComputation, mergedSavedParms));
        setRunSucceeded(false);
        setRunError(null);
        setRunResult(null);
        return saved;
      } catch (error) {
        console.error("Failed to save computation", error);
        setSaveError(
          error instanceof Error ? error.message : t("computations:editor.save_failed"),
        );
        throw error;
      } finally {
        setSaving(false);
      }
    },
    [localParms, processOptions, groupOptions, saveAction, providedAlgorithm, t],
  );

  const runComputation = useCallback(async () => {
    if (!canRunComputation) {
      return;
    }

    const start = new Date(runStart);
    const end = new Date(runEnd);
    if (!Number.isFinite(start.getTime()) || !Number.isFinite(end.getTime())) {
      setRunSucceeded(false);
      setRunError(t("computations:editor.run_invalid_dates"));
      return;
    }
    if (start >= end) {
      setRunSucceeded(false);
      setRunError(t("computations:editor.run_invalid_window"));
      return;
    }

    setRunning(true);
    setRunError(null);
    setRunSucceeded(false);
    setRunResult(null);
    try {
      const result = await Promise.resolve(
        runAction?.(activeComputationId, start, end),
      );
      if (result) {
        setRunResult(result);
      }
      setRunSucceeded(true);
    } catch (error) {
      console.error(`Failed to run computation ${activeComputationId}`, error);
      setRunError(t("computations:editor.run_failed"));
    } finally {
      setRunning(false);
    }
  }, [activeComputationId, canRunComputation, runEnd, runAction, runStart, t]);

  const timeSeriesByKey = useMemo(() => {
    const byKey = new Map<
      number,
      NonNullable<ComputationRunResult["series"]>[number]
    >();
    (runResult?.series ?? []).forEach((series) => {
      const key = series.tsid?.key;
      if (key !== undefined) {
        byKey.set(key, series);
      }
    });
    return byKey;
  }, [runResult]);

  const inputChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      const { name, value } = event.target;
      dispatch({ type: "save", payload: { [name]: value } });
    },
    [dispatch],
  );

  const enabledChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      dispatch({ type: "save", payload: { enabled: event.target.checked } });
    },
    [dispatch],
  );

  const processSelectValue = useMemo(() => {
    return toSelectValue(
      localComputation.appId,
      localComputation.applicationName,
      processOptions,
    );
  }, [localComputation.appId, localComputation.applicationName, processOptions]);

  const groupSelectValue = useMemo(() => {
    return toSelectValue(
      localComputation.groupId,
      localComputation.groupName,
      groupOptions,
    );
  }, [localComputation.groupId, localComputation.groupName, groupOptions]);

  const processChange = useCallback(
    (event: React.ChangeEvent<HTMLSelectElement>) => {
      const raw = event.target.value;
      if (!raw) {
        dispatch({ type: "save", payload: { appId: undefined, applicationName: "" } });
        return;
      }
      const appId = Number.parseInt(raw, 10);
      const selected = processOptions.find((app) => app.appId === appId);
      dispatch({
        type: "save",
        payload: {
          appId,
          applicationName: selected?.appName ?? "",
        },
      });
    },
    [dispatch, processOptions],
  );

  const groupChange = useCallback(
    (event: React.ChangeEvent<HTMLSelectElement>) => {
      const raw = event.target.value;
      if (!raw) {
        dispatch({ type: "save", payload: { groupId: undefined, groupName: "" } });
        return;
      }
      const groupId = Number.parseInt(raw, 10);
      const selected = groupOptions.find((group) => group.groupId === groupId);
      dispatch({
        type: "save",
        payload: {
          groupId,
          groupName: selected?.groupName ?? "",
        },
      });
    },
    [dispatch, groupOptions],
  );

  useEffect(() => {
    if (!edit) {
      return;
    }

    onDraftChange?.({
      ...localComputation,
      props: { ...(localComputation.props ?? {}) },
      parmList: localParms,
    });
  }, [edit, localComputation, localParms, onDraftChange]);

  return (
    <Card>
      <Card.Body>
        <Row className="g-3">
          <Col xs={12}>
            <Row>
              <Col>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={3} htmlFor="compName">
                    {t("computations:editor.name")}
                  </Form.Label>
                  <Col sm={9}>
                    <Form.Control
                      type="text"
                      id="compName"
                      name="name"
                      readOnly={!edit}
                      value={localComputation.name ?? ""}
                      onChange={inputChange}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={3} htmlFor="algorithmName">
                    {t("computations:editor.algorithmName")}
                  </Form.Label>
                  <Col sm={9}>
                    <div className="d-flex gap-2">
                      <Form.Control
                        type="text"
                        id="algorithmName"
                        name="algorithmName"
                        readOnly={true}
                        value={localComputation.algorithmName ?? ""}
                      />
                      {edit && (
                        <Button
                          variant="outline-secondary"
                          onClick={() =>
                            onSelectAlgorithm?.({
                              ...localComputation,
                              props: { ...(localComputation.props ?? {}) },
                              parmList: localParms,
                            })
                          }
                          aria-label={t("computations:editor.select_algorithm")}
                        >
                          {t("computations:editor.select_algorithm")}
                        </Button>
                      )}
                    </div>
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={3} htmlFor="applicationName">
                    {t("computations:editor.applicationName")}
                  </Form.Label>
                  <Col sm={9}>
                    <Form.Select
                      id="applicationName"
                      name="appId"
                      disabled={!edit}
                      value={processSelectValue}
                      onChange={processChange}
                    >
                      <option value="" />
                      {processOptions.map((app) => (
                        <option key={app.appId} value={String(app.appId ?? "")}>
                          {app.appName}
                        </option>
                      ))}
                    </Form.Select>
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={3} htmlFor="groupName">
                    {t("computations:editor.groupName")}
                  </Form.Label>
                  <Col sm={9}>
                    <Form.Select
                      id="groupName"
                      name="groupId"
                      disabled={!edit}
                      value={groupSelectValue}
                      onChange={groupChange}
                    >
                      <option value="" />
                      {groupOptions.map((g) => (
                        <option key={g.groupId} value={String(g.groupId ?? "")}>
                          {g.groupName}
                        </option>
                      ))}
                    </Form.Select>
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={3} htmlFor="enabled">
                    {t("computations:editor.enabled")}
                  </Form.Label>
                  <Col sm={9}>
                    <Form.Check
                      id="enabled"
                      name="enabled"
                      disabled={!edit}
                      checked={localComputation.enabled ?? false}
                      onChange={enabledChange}
                    />
                  </Col>
                </FormGroup>
              </Col>
              <Col>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={3} htmlFor="comment">
                    {t("computations:editor.description")}
                  </Form.Label>
                  <Col sm={9}>
                    <Form.Control
                      as="textarea"
                      rows={4}
                      id="comment"
                      name="comment"
                      readOnly={!edit}
                      value={localComputation.comment ?? ""}
                      onChange={inputChange}
                    />
                  </Col>
                </FormGroup>
              </Col>
            </Row>
          </Col>
          <Col xs={12}>
            <ComputationParamsTable
              parms={localParms}
              edit={edit}
              onAdd={(parm) => setLocalParms((prev) => [...prev, parm])}
              onRemove={(roleName) =>
                setLocalParms((prev) =>
                  prev.filter((p) => (p.algoRoleName ?? "") !== roleName),
                )
              }
              onUpdate={(oldRoleName, newParm) =>
                setLocalParms((prev) =>
                  prev.map((p) =>
                    (p.algoRoleName ?? "") === oldRoleName ? newParm : p,
                  ),
                )
              }
            />
          </Col>
          <Col xs={12} xl={5}>
            <PropertiesTable
              theProps={props}
              actions={propertyActions}
              edit={edit}
              canAdd={true}
              width={"100%"}
              height={"auto"}
            />
          </Col>
        </Row>
        {canShowRunControls && (
          <Row className="mt-3">
            {edit && canRunComputation && hasUnsavedChanges && (
              <Col xs={12}>
                <Alert className="mb-0 py-2" variant="warning">
                  {t("computations:editor.run_uses_saved")}
                </Alert>
              </Col>
            )}
            {!canRunComputation && (
              <Col xs={12}>
                <Alert className="mb-0 py-2" variant="info">
                  {t("computations:editor.run_save_first")}
                </Alert>
              </Col>
            )}
            <Col className="d-flex flex-wrap align-items-end gap-2">
              <Form.Group controlId={`run-start-${activeComputationId}`}>
                <Form.Label>{t("computations:editor.run_start")}</Form.Label>
                <Form.Control
                  type="datetime-local"
                  value={runStart}
                  onChange={(event) => {
                    setRunStart(event.target.value);
                    setRunSucceeded(false);
                    setRunError(null);
                    setRunResult(null);
                  }}
                />
              </Form.Group>
              <Form.Group controlId={`run-end-${activeComputationId}`}>
                <Form.Label>{t("computations:editor.run_end")}</Form.Label>
                <Form.Control
                  type="datetime-local"
                  value={runEnd}
                  onChange={(event) => {
                    setRunEnd(event.target.value);
                    setRunSucceeded(false);
                    setRunError(null);
                    setRunResult(null);
                  }}
                />
              </Form.Group>
              <Button
                type="button"
                variant="success"
                disabled={running || !canRunComputation}
                onClick={() => {
                  void runComputation();
                }}
                aria-label={t("computations:editor.run_for", {
                  id: activeComputationId,
                })}
              >
                <PlayFill />{" "}
                {running
                  ? t("computations:editor.running")
                  : t("computations:editor.run")}
              </Button>
            </Col>
            {(runError || runSucceeded) && (
              <Col xs={12} className="mt-2">
                <Alert className="mb-0 py-2" variant={runError ? "danger" : "success"}>
                  {runError ?? t("computations:editor.run_succeeded")}
                </Alert>
              </Col>
            )}
            {runResult && (
              <Col xs={12} className="mt-2">
                <Card>
                  <Card.Body>
                    <Card.Title as="h5">
                      {t("computations:editor.results_title")}
                    </Card.Title>
                    {runResult.messages.length > 0 && (
                      <div className="mb-2 small text-body-secondary">
                        {runResult.messages.slice(-3).map((message, idx) => (
                          <div key={`${message}-${idx}`}>{message}</div>
                        ))}
                      </div>
                    )}
                    {runResult.errors.length > 0 && (
                      <Alert className="py-2" variant="warning">
                        {runResult.errors.join(" ")}
                      </Alert>
                    )}
                    {(runResult.results?.tsIds ?? []).length === 0 ? (
                      <Alert className="mb-0 py-2" variant="info">
                        {t("computations:editor.results_no_outputs")}
                      </Alert>
                    ) : (
                      <Table responsive bordered hover size="sm" className="mb-0">
                        <thead>
                          <tr>
                            <th>{t("computations:editor.results_tsid")}</th>
                            <th>{t("computations:editor.results_key")}</th>
                            <th>{t("computations:editor.results_values")}</th>
                            <th>{t("computations:editor.results_latest_time")}</th>
                            <th>{t("computations:editor.results_latest_value")}</th>
                            <th>{t("computations:editor.results_units")}</th>
                          </tr>
                        </thead>
                        <tbody>
                          {(runResult.results?.tsIds ?? []).map((tsid, idx) => {
                            const key = tsid.key;
                            const series =
                              key !== undefined ? timeSeriesByKey.get(key) : undefined;
                            const values = series?.values ?? [];
                            const latest =
                              values.length > 0 ? values[values.length - 1] : undefined;
                            return (
                              <tr key={`${tsid.uniqueString ?? "output"}-${idx}`}>
                                <td>{tsid.uniqueString ?? ""}</td>
                                <td>{key ?? ""}</td>
                                <td>{values.length}</td>
                                <td>
                                  {latest?.sampleTime
                                    ? new Date(latest.sampleTime).toLocaleString()
                                    : ""}
                                </td>
                                <td>{latest?.value ?? ""}</td>
                                <td>
                                  {tsid.storageUnits ??
                                    series?.tsid?.storageUnits ??
                                    ""}
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </Table>
                    )}
                  </Card.Body>
                </Card>
              </Col>
            )}
          </Row>
        )}
        {edit && (
          <Row className="mt-3">
            {saveError && (
              <Col xs={12} className="mb-2">
                <Alert className="mb-0 py-2" variant="danger">
                  {saveError}
                </Alert>
              </Col>
            )}
            <Col className="d-flex justify-content-end gap-2">
              <Button
                type="button"
                onClick={() => {
                  if (activeComputationId !== undefined) {
                    cancelAction?.(activeComputationId);
                  }
                }}
                variant="secondary"
                aria-label={t("computations:editor.cancel_for", {
                  id: activeComputationId,
                })}
              >
                <X /> {t("translation:cancel")}
              </Button>
              <Button
                type="button"
                onClick={() => {
                  void saveComputation(localComputation);
                }}
                variant="primary"
                disabled={saving}
                aria-label={t("computations:editor.save_for", {
                  id: activeComputationId,
                })}
              >
                <Save />{" "}
                {saving ? t("computations:editor.saving") : t("translation:save")}
              </Button>
            </Col>
          </Row>
        )}
      </Card.Body>
    </Card>
  );
};

export default Computation;
