import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  FormGroup,
  InputGroup,
  Placeholder,
  Row,
} from "react-bootstrap";
import { PropertiesTable, type Property } from "../../../components/properties";
import { use, useCallback, useMemo, useReducer, useState } from "react";
import type {
  ApiAlgorithm,
  ApiAlgorithmRef,
  ApiAppRef,
  ApiCompParm,
  ApiComputation,
  ApiTsGroupRef,
} from "opendcs-api";
import { useTranslation } from "react-i18next";
import type {
  CancelAction,
  CollectionActions,
  SaveAction,
} from "../../../util/Actions";
import { Save, X } from "react-bootstrap-icons";
import { ComputationReducer } from "./ComputationReducer";
import { ComputationParamsTable } from "./ComputationParamsTable";
import { AlgorithmSelectModal } from "./AlgorithmSelectModal";
import SinceUntilEditor from "../../../components/controls/SinceUntilEditor";
import {
  apiTimeToEditorValue,
  editorValueToStartFields,
  editorValueToEndFields,
} from "./computationTime";

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
  algorithm?: Promise<ApiAlgorithm | undefined> | ApiAlgorithm;
  getAlgorithm?: (algorithmId: number) => Promise<ApiAlgorithm>;
  actions?: SaveAction<ApiComputation> & CancelAction<number>;
  edit?: boolean;
  processOptions?: ApiAppRef[];
  groupOptions?: ApiTsGroupRef[];
}

const roleKey = (roleName: string | undefined): string =>
  (roleName ?? "").trim().toLowerCase();

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

const assignedId = (value: number | undefined): number | undefined =>
  value !== undefined && value > 0 ? value : undefined;

const prepareParmForSave = (parm: ApiCompParm): ApiCompParm => {
  const dataType = parm.dataType?.trim();
  return {
    ...parm,
    tsKey: assignedId(parm.tsKey),
    siteId: assignedId(parm.siteId),
    dataType: dataType && dataType.length > 0 ? dataType : undefined,
    dataTypeId:
      dataType && dataType.length > 0 ? assignedId(parm.dataTypeId) : undefined,
  };
};

export const Computation: React.FC<ComputationProperties> = ({
  computation,
  algorithm,
  getAlgorithm,
  actions = {},
  edit = false,
  processOptions = [],
  groupOptions = [],
}) => {
  const { t } = useTranslation(["computations", "translation"]);
  const providedComputation =
    computation instanceof Promise ? use(computation) : computation;
  const providedAlgorithm = algorithm instanceof Promise ? use(algorithm) : algorithm;
  const [localComputation, dispatch] = useReducer(
    ComputationReducer,
    providedComputation,
  );
  const [localParms, setLocalParms] = useState<ApiCompParm[]>(() =>
    mergeRequiredParms(
      providedComputation.parmList ?? [],
      requiredParmsFromAlgorithm(providedAlgorithm),
    ),
  );
  const [showAlgorithmModal, setShowAlgorithmModal] = useState(false);
  const [nameError, setNameError] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const handleAlgorithmSelected = useCallback(
    async (ref: ApiAlgorithmRef) => {
      let payload: UiComputation = {
        algorithmName: ref.algorithmName,
        algorithmId: ref.algorithmId,
      };
      if (!getAlgorithm || !ref.algorithmId) {
        dispatch({ type: "save", payload });
        return;
      }
      try {
        const fullAlgo = await getAlgorithm(ref.algorithmId);
        payload = {
          ...payload,
          algorithmName: fullAlgo.name ?? ref.algorithmName,
          ...(localComputation.comment ? {} : { comment: fullAlgo.description }),
          props: fullAlgo.props
            ? { ...fullAlgo.props, ...localComputation.props }
            : localComputation.props,
        };
        setLocalParms((prev) =>
          mergeRequiredParms(prev, requiredParmsFromAlgorithm(fullAlgo)),
        );
        dispatch({ type: "save", payload });
      } catch (e) {
        console.warn("Failed to load full algorithm after selection", e);
        dispatch({ type: "save", payload });
      }
    },
    [getAlgorithm, localComputation.comment, localComputation.props],
  );

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
      if (!comp.name?.trim()) {
        setNameError(true);
        return;
      }
      setNameError(false);
      setSaveError(null);
      try {
        await actions.save?.({
          ...(comp as ApiComputation),
          parmList: localParms.map(prepareParmForSave),
        });
      } catch (err) {
        console.warn("Computation save failed", err);
        setSaveError(t("computations:editor.save_error"));
      }
    },
    [actions, localParms, t],
  );

  const inputChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      const { name, value } = event.target;
      dispatch({ type: "save", payload: { [name]: value } });
    },
    [dispatch],
  );

  const selectChange = useCallback(
    (event: React.ChangeEvent<HTMLSelectElement>) => {
      const { name, value } = event.target;
      dispatch({ type: "save", payload: { [name]: value } });
    },
    [dispatch],
  );

  // The backend persists the computation/group link by groupId (a DB foreign key),
  // so selecting a group by name must also resolve and store its groupId.
  const groupChange = useCallback(
    (event: React.ChangeEvent<HTMLSelectElement>) => {
      const groupName = event.target.value;
      const match = groupOptions.find((g) => g.groupName === groupName);
      dispatch({
        type: "save",
        payload: {
          groupName: groupName || undefined,
          groupId: groupName ? match?.groupId : undefined,
        },
      });
    },
    [dispatch, groupOptions],
  );

  const enabledChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      dispatch({ type: "save", payload: { enabled: event.target.checked } });
    },
    [dispatch],
  );

  const handleSinceChange = useCallback(
    (value: string) => {
      dispatch({ type: "save", payload: editorValueToStartFields(value) });
    },
    [dispatch],
  );

  const handleUntilChange = useCallback(
    (value: string) => {
      dispatch({ type: "save", payload: editorValueToEndFields(value) });
    },
    [dispatch],
  );

  const sinceValue = apiTimeToEditorValue(
    localComputation.effectiveStartType,
    localComputation.effectiveStartDate,
    localComputation.effectiveStartInterval,
  );
  const untilValue = apiTimeToEditorValue(
    localComputation.effectiveEndType,
    localComputation.effectiveEndDate,
    localComputation.effectiveEndInterval,
  );

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
                      isInvalid={nameError}
                      onChange={(e) => {
                        if (nameError) setNameError(false);
                        inputChange(e);
                      }}
                    />
                    <Form.Control.Feedback type="invalid">
                      A name is required.
                    </Form.Control.Feedback>
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={3} htmlFor="algorithmName">
                    {t("computations:editor.algorithmName")}
                  </Form.Label>
                  <Col sm={9}>
                    {edit ? (
                      <InputGroup>
                        <Form.Control
                          type="text"
                          id="algorithmName"
                          name="algorithmName"
                          readOnly
                          value={localComputation.algorithmName ?? ""}
                          placeholder="No algorithm selected"
                        />
                        <Button
                          variant="secondary"
                          className="dt-button"
                          onClick={() => setShowAlgorithmModal(true)}
                          aria-label={t("computations:editor.select_algorithm_title")}
                        >
                          {t("translation:choose")}
                        </Button>
                      </InputGroup>
                    ) : (
                      <Form.Control
                        type="text"
                        id="algorithmName"
                        readOnly
                        value={localComputation.algorithmName ?? ""}
                      />
                    )}
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={3} htmlFor="applicationName">
                    {t("computations:editor.applicationName")}
                  </Form.Label>
                  <Col sm={9}>
                    <Form.Select
                      id="applicationName"
                      name="applicationName"
                      disabled={!edit}
                      value={localComputation.applicationName ?? ""}
                      onChange={selectChange}
                    >
                      <option value="" />
                      {processOptions.map((app) => (
                        <option key={app.appId} value={app.appName ?? ""}>
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
                      name="groupName"
                      disabled={!edit}
                      value={localComputation.groupName ?? ""}
                      onChange={groupChange}
                    >
                      <option value="" />
                      {groupOptions.map((g) => (
                        <option key={g.groupId} value={g.groupName ?? ""}>
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
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={3} htmlFor="effective-start-method">
                    {t("computations:editor.effective_start")}
                  </Form.Label>
                  <Col sm={9}>
                    <SinceUntilEditor
                      kind="since"
                      value={sinceValue}
                      edit={edit}
                      onChange={handleSinceChange}
                      allowNoLimit
                      idPrefix="effective-start"
                      labels={{
                        method: t("computations:editor.effective_start"),
                        noLimit: t("computations:editor.no_limit"),
                        now: t("computations:editor.now"),
                        nowMinus: t("computations:editor.now_minus"),
                        amount: t("computations:editor.interval"),
                        calendar: t("computations:editor.calendar"),
                      }}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={3} htmlFor="effective-end-method">
                    {t("computations:editor.effective_end")}
                  </Form.Label>
                  <Col sm={9}>
                    <SinceUntilEditor
                      kind="until"
                      value={untilValue}
                      edit={edit}
                      onChange={handleUntilChange}
                      allowNoLimit
                      idPrefix="effective-end"
                      labels={{
                        method: t("computations:editor.effective_end"),
                        noLimit: t("computations:editor.no_limit"),
                        now: t("computations:editor.now"),
                        nowMinus: t("computations:editor.now_minus"),
                        amount: t("computations:editor.interval"),
                        calendar: t("computations:editor.calendar"),
                      }}
                    />
                  </Col>
                </FormGroup>
                {localComputation.lastModified && (
                  <FormGroup as={Row} className="mb-3">
                    <Form.Label column sm={3} htmlFor="lastModified">
                      {t("computations:editor.last_modified")}
                    </Form.Label>
                    <Col sm={9}>
                      <Form.Control
                        plaintext
                        readOnly
                        id="lastModified"
                        value={new Date(localComputation.lastModified).toLocaleString()}
                      />
                    </Col>
                  </FormGroup>
                )}
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
        {saveError && (
          <Alert
            variant="danger"
            dismissible
            onClose={() => setSaveError(null)}
            className="mt-3"
          >
            {saveError}
          </Alert>
        )}
        {edit && (
          <Row className="mt-3">
            <Col className="d-flex justify-content-end gap-2">
              <Button
                onClick={() => {
                  actions.cancel?.(providedComputation.computationId!);
                }}
                variant="secondary"
                aria-label={t("computations:editor.cancel_for", {
                  id: providedComputation.computationId,
                })}
              >
                <X /> {t("translation:cancel")}
              </Button>
              <Button
                onClick={async () => {
                  await saveComputation(localComputation);
                }}
                variant="primary"
                aria-label={t("computations:editor.save_for", {
                  id: localComputation.computationId,
                })}
              >
                <Save /> {t("translation:save")}
              </Button>
            </Col>
          </Row>
        )}
      </Card.Body>
      <AlgorithmSelectModal
        show={showAlgorithmModal}
        onHide={() => setShowAlgorithmModal(false)}
        onSelect={handleAlgorithmSelected}
      />
    </Card>
  );
};

export default Computation;
