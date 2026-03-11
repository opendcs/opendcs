import { Button, Card, Col, Form, FormGroup, Placeholder, Row } from "react-bootstrap";
import { PropertiesTable, type Property } from "../../../components/properties";
import { use, useCallback, useMemo, useReducer, useState } from "react";
import type { ApiAlgorithm, ApiPropSpec } from "opendcs-api";
import { useTranslation } from "react-i18next";
import type {
  CancelAction,
  CollectionActions,
  SaveAction,
} from "../../../util/Actions";
import { AlgorithmReducer } from "./AlgorithmReducer";
import { Save, X } from "react-bootstrap-icons";
import { AlgorithmParamsTable, type AlgoParm } from "./AlgorithmParamsTable";

export type UiAlgorithm = Partial<ApiAlgorithm>;

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };
const SEARCH_H = { height: "1.875rem" };
const INFO_H = { height: "0.8rem" };

export const AlgorithmSkeleton: React.FC<{ edit?: boolean }> = ({ edit = false }) => (
  <Card>
    <Card.Body>
      <Row>
        {/* Left column — Name, ExecClass, Parameters table */}
        <Col>
          {/* Name */}
          <Row className="mb-3 align-items-center">
            <Placeholder as={Col} sm={3} animation="glow">
              <Placeholder xs={7} className="rounded" style={LABEL_H} />
            </Placeholder>
            <Placeholder as={Col} sm={9} animation="glow">
              <Placeholder xs={12} className="rounded" style={INPUT_H} />
            </Placeholder>
          </Row>
          {/* ExecClass */}
          <Row className="mb-3 align-items-center">
            <Placeholder as={Col} sm={3} animation="glow">
              <Placeholder xs={9} className="rounded" style={LABEL_H} />
            </Placeholder>
            <Placeholder as={Col} sm={9} animation="glow">
              <Placeholder xs={12} className="rounded" style={INPUT_H} />
            </Placeholder>
          </Row>
          {/* Parameters table */}
          {/* Top: [show entries] [+ button (edit)] ... [search] */}
          <div className="d-flex justify-content-between align-items-center mb-2">
            <div className="d-flex align-items-center gap-2">
              {edit && (
                <Placeholder animation="glow">
                  <Placeholder
                    className="rounded"
                    style={{ ...SEARCH_H, width: "2.25rem" }}
                  />
                </Placeholder>
              )}
            </div>
            <Placeholder animation="glow" style={{ width: "25%" }}>
              <Placeholder xs={12} className="rounded" style={SEARCH_H} />
            </Placeholder>
          </div>
          <Placeholder
            as="div"
            animation="glow"
            className="mb-1 d-flex justify-content-center"
          >
            <Placeholder xs={4} className="rounded" style={LABEL_H} />
          </Placeholder>
          <Placeholder animation="glow">
            <Placeholder xs={12} className="rounded" style={{ height: "4rem" }} />
          </Placeholder>
          {/* Bottom: [info text] */}
          <div className="d-flex justify-content-between align-items-center mt-1">
            <Placeholder animation="glow" style={{ width: "35%" }}>
              <Placeholder xs={12} className="rounded" style={INFO_H} />
            </Placeholder>
          </div>
        </Col>

        {/* Right column — Description, PropertiesTable */}
        <Col>
          {/* Description */}
          <Row className="mb-3 align-items-start">
            <Placeholder as={Col} sm={3} animation="glow">
              <Placeholder xs={10} className="rounded" style={LABEL_H} />
            </Placeholder>
            <Placeholder as={Col} sm={9} animation="glow">
              <Placeholder xs={12} className="rounded" style={{ height: "5.75rem" }} />
            </Placeholder>
          </Row>
          {/* Properties table */}
          {/* Top: [+ button (edit)] ... [search] */}
          <div className="d-flex justify-content-between align-items-center mb-2">
            <div className="d-flex align-items-center gap-2">
              {edit && (
                <Placeholder animation="glow">
                  <Placeholder
                    className="rounded"
                    style={{ ...SEARCH_H, width: "2.25rem" }}
                  />
                </Placeholder>
              )}
            </div>
            <Placeholder animation="glow" style={{ width: "25%" }}>
              <Placeholder xs={12} className="rounded" style={SEARCH_H} />
            </Placeholder>
          </div>
          {/* Properties table caption */}
          <Placeholder
            as="div"
            animation="glow"
            className="mb-1 d-flex justify-content-center"
          >
            <Placeholder xs={4} className="rounded" style={LABEL_H} />
          </Placeholder>
          {/* Properties table block with scroll */}
          <Placeholder animation="glow">
            <Placeholder
              xs={12}
              className="rounded"
              style={{ height: "calc(10 * 2rem)" }}
            />
          </Placeholder>
          {/* Bottom: [info text] */}
          <div className="d-flex justify-content-start align-items-center mt-1">
            <Placeholder animation="glow" style={{ width: "35%" }}>
              <Placeholder xs={12} className="rounded" style={INFO_H} />
            </Placeholder>
          </div>
        </Col>
      </Row>

      {/* Edit-mode footer */}
      {edit && (
        <Row className="mt-2">
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

export interface AlgorithmProperties {
  algorithm: Promise<UiAlgorithm> | UiAlgorithm;
  propSpecs?: Promise<ApiPropSpec[]> | ApiPropSpec[];
  initialParms?: Promise<AlgoParm[]> | AlgoParm[];
  actions?: SaveAction<ApiAlgorithm> & CancelAction<number>;
  edit?: boolean;
}

export const Algorithm: React.FC<AlgorithmProperties> = ({
  algorithm,
  propSpecs,
  initialParms = [],
  actions = {},
  edit = false,
}) => {
  const { t } = useTranslation(["algorithms", "translation"]);
  const providedAlgorithm = algorithm instanceof Promise ? use(algorithm) : algorithm;
  const resolvedSpecs: ApiPropSpec[] = propSpecs
    ? propSpecs instanceof Promise
      ? use(propSpecs)
      : propSpecs
    : [];
  const resolvedParms: AlgoParm[] = initialParms
    ? initialParms instanceof Promise
      ? use(initialParms)
      : initialParms
    : [];
  const [localAlgorithm, dispatch] = useReducer(AlgorithmReducer, providedAlgorithm);
  const [localParms, setLocalParms] = useState<AlgoParm[]>(resolvedParms);

  const props = useMemo(() => {
    const saved = localAlgorithm.props || {};
    // Start with all propspec-defined properties (using saved value if present, else empty)
    const specProps: Property[] = resolvedSpecs.map((spec) => ({
      name: spec.name!,
      value: saved[spec.name!] ?? "",
      spec,
    }));
    // Add any saved props that have no matching spec
    const specNames = new Set(resolvedSpecs.map((s) => s.name!));
    const extraProps: Property[] = Object.entries(saved)
      .filter(([k]) => !specNames.has(k))
      .map(([k, v]) => ({ name: k, value: v }));
    return [...specProps, ...extraProps];
  }, [localAlgorithm.props, resolvedSpecs]);

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

  const saveAlgorithm = useCallback(
    (algo: UiAlgorithm) => {
      actions.save!({ ...algo, parms: localParms });
    },
    [actions, localParms],
  );

  const inputChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      const { name, value } = event.target;
      dispatch({ type: "save", payload: { [name]: value } });
    },
    [dispatch],
  );

  return (
    <Card>
      <Card.Body>
        <Row>
          {/* Left column — Name, ExecClass, Parameters table */}
          <Row>
            <Col>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column lg={3} htmlFor="algoName">
                  {t("algorithms:editor.name")}
                </Form.Label>
                <Col lg={9}>
                  <Form.Control
                    type="text"
                    id="algoName"
                    name="name"
                    readOnly={!edit}
                    defaultValue={localAlgorithm.name}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column lg={3} htmlFor="execClass">
                  {t("algorithms:editor.execClass")}
                </Form.Label>
                <Col lg={9}>
                  <Form.Control
                    type="text"
                    id="execClass"
                    name="execClass"
                    readOnly={!edit}
                    defaultValue={localAlgorithm.execClass}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
            </Col>
            {/* Right column — Description, Properties table */}
            <Col>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column lg={3} htmlFor="algoDescription">
                  {t("algorithms:editor.description")}
                </Form.Label>
                <Col lg={9}>
                  <Form.Control
                    as="textarea"
                    rows={3}
                    id="algoDescription"
                    name="description"
                    readOnly={!edit}
                    defaultValue={localAlgorithm.description}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
            </Col>
          </Row>
          <Col>
            <AlgorithmParamsTable
              parms={localParms}
              edit={edit}
              onAdd={(parm) => setLocalParms((prev) => [...prev, parm])}
              onRemove={(roleName) =>
                setLocalParms((prev) => prev.filter((p) => p.roleName !== roleName))
              }
              onUpdate={(oldRoleName, newParm) =>
                setLocalParms((prev) =>
                  prev.map((p) => (p.roleName === oldRoleName ? newParm : p)),
                )
              }
            />
          </Col>
          <Col>
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
        {edit && (
          <Row className="mt-3">
            <Col className="d-flex justify-content-end gap-2">
              <Button
                onClick={() => {
                  actions.cancel?.(providedAlgorithm.algorithmId!);
                }}
                variant="secondary"
                aria-label={t("algorithms:editor.cancel_for", {
                  id: providedAlgorithm.algorithmId,
                })}
              >
                <X /> {t("translation:cancel")}
              </Button>
              <Button
                onClick={() => saveAlgorithm(localAlgorithm)}
                variant="primary"
                aria-label={t("algorithms:editor.save_for", {
                  id: localAlgorithm.algorithmId,
                })}
              >
                <Save /> {t("translation:save")}
              </Button>
            </Col>
          </Row>
        )}
      </Card.Body>
    </Card>
  );
};

export default Algorithm;
