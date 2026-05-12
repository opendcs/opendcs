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
import { DetailFade } from "../../../components/data-table";

export type UiAlgorithm = Partial<ApiAlgorithm>;

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };
const SEARCH_H = { height: "1.875rem" };
const INFO_H = { height: "0.8rem" };

// Mirror one DataTable's chrome:
//   [ (+) ................................... [search] ]   toolbar
//                       <caption>
//   +-----------------------------------------------+
//   | col | col | col |  ... thead                  |
//   |-----------------------------------------------|
//   |                                               |
//   |   scrolling tbody (height = scrollY)          |
//   |                                               |
//   +-----------------------------------------------+
//                                       N of N entries   info
const SkeletonTable: React.FC<{
  edit: boolean;
  cols: number;
  infoJustify: "between" | "start";
}> = ({ edit, cols, infoJustify }) => (
  <>
    {/* Toolbar row 1 (edit only): [+ button] — DataTables `top1Start` sits
        in a row above the default `topEnd` search bar. */}
    {edit && (
      <div className="d-flex justify-content-start mb-2">
        <Placeholder animation="glow">
          <Placeholder className="rounded" style={{ ...SEARCH_H, width: "2.25rem" }} />
        </Placeholder>
      </div>
    )}
    {/* Toolbar row 2: [search] */}
    <div className="d-flex justify-content-end align-items-center mb-2">
      <Placeholder animation="glow" style={{ width: "25%" }}>
        <Placeholder xs={12} className="rounded" style={SEARCH_H} />
      </Placeholder>
    </div>
    {/* Caption */}
    <Placeholder
      as="div"
      animation="glow"
      className="mb-1 d-flex justify-content-center"
    >
      <Placeholder xs={4} className="rounded" style={LABEL_H} />
    </Placeholder>
    {/* Table shell — matches DataTable className="border" */}
    <div className="border rounded overflow-hidden">
      {/* thead row with a label per column */}
      <div
        className="d-flex align-items-center px-2"
        style={{ height: "2rem", background: "var(--odcs-table-header-bg)" }}
      >
        {Array.from({ length: cols + (edit ? 1 : 0) }).map((_, i) => (
          // Column placeholders are indistinguishable — index is the stable
          // identity here (list never reorders).
          <Placeholder key={`thead-${i}`} animation="glow" className="flex-fill me-2">
            {" "}
            {/* NOSONAR — fixed-length skeleton list never reorders */}
            <Placeholder xs={6} className="rounded" style={{ height: "0.75rem" }} />
          </Placeholder>
        ))}
      </div>
      {/* Scrolling tbody (matches DataTables `scrollY: calc(10 * 2rem)`) */}
      <Placeholder animation="glow" className="d-block">
        <Placeholder xs={12} style={{ height: "calc(10 * 2rem)" }} />
      </Placeholder>
    </div>
    {/* Footer: info text */}
    <div className={`d-flex justify-content-${infoJustify} align-items-center mt-1`}>
      <Placeholder animation="glow" style={{ width: "35%" }}>
        <Placeholder xs={12} className="rounded" style={INFO_H} />
      </Placeholder>
    </div>
  </>
);

export const AlgorithmSkeleton: React.FC<{ edit?: boolean; className?: string }> = ({
  edit = false,
  className,
}) => (
  <Card
    className={["algorithm-card", edit ? "algorithm-card--edit" : null, className]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        {/* Top band — Name/ExecClass (left) | Description (right) */}
        <Row>
          <Col>
            {/* Name */}
            <Row className="mb-3 align-items-center">
              <Placeholder as={Col} lg={3} animation="glow">
                <Placeholder xs={7} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} lg={9} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
            {/* ExecClass */}
            <Row className="mb-3 align-items-center">
              <Placeholder as={Col} lg={3} animation="glow">
                <Placeholder xs={9} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} lg={9} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
          </Col>
          <Col>
            {/* Description */}
            <Row className="mb-3 align-items-start">
              <Placeholder as={Col} lg={3} animation="glow">
                <Placeholder xs={10} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} lg={9} animation="glow">
                <Placeholder
                  xs={12}
                  className="rounded"
                  style={{ height: "5.75rem" }}
                />
              </Placeholder>
            </Row>
          </Col>
        </Row>
        {/* Bottom band — Parameters table | Properties table */}
        <Col>
          <SkeletonTable edit={edit} cols={2} infoJustify="between" />
        </Col>
        <Col>
          <SkeletonTable edit={edit} cols={2} infoJustify="start" />
        </Col>
      </Row>

      {/* Edit-mode footer */}
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

  let resolvedSpecs: ApiPropSpec[];
  if (!propSpecs) resolvedSpecs = [];
  else if (propSpecs instanceof Promise) resolvedSpecs = use(propSpecs);
  else resolvedSpecs = propSpecs;

  let resolvedParms: AlgoParm[];
  if (!initialParms) resolvedParms = [];
  else if (initialParms instanceof Promise) resolvedParms = use(initialParms);
  else resolvedParms = initialParms;
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
    <DetailFade skeleton={<AlgorithmSkeleton edit={edit} />}>
      <Card
        className={["algorithm-card", edit ? "algorithm-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
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
    </DetailFade>
  );
};

export default Algorithm;
