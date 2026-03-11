import { Button, Card, Col, Form, FormGroup, Placeholder, Row } from "react-bootstrap";
import { PropertiesTable, type Property } from "../../../components/properties";
import { use, useCallback, useMemo, useReducer } from "react";
import type { ApiComputation } from "opendcs-api";
import { useTranslation } from "react-i18next";
import type {
  CancelAction,
  CollectionActions,
  SaveAction,
} from "../../../util/Actions";
import { Save, X } from "react-bootstrap-icons";
import { ComputationReducer } from "./ComputationReducer";

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
  actions?: SaveAction<ApiComputation> & CancelAction<number>;
  edit?: boolean;
}

export const Computation: React.FC<ComputationProperties> = ({
  computation,
  actions = {},
  edit = false,
}) => {
  const { t } = useTranslation(["computations", "translation"]);
  const providedComputation =
    computation instanceof Promise ? use(computation) : computation;
  const [localComputation, dispatch] = useReducer(
    ComputationReducer,
    providedComputation,
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
    (comp: UiComputation) => {
      actions.save?.(comp as ApiComputation);
    },
    [actions],
  );

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

  return (
    <Card>
      <Card.Body>
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
                  defaultValue={localComputation.name}
                  onChange={inputChange}
                />
              </Col>
            </FormGroup>
            <FormGroup as={Row} className="mb-3">
              <Form.Label column sm={3} htmlFor="algorithmName">
                {t("computations:editor.algorithmName")}
              </Form.Label>
              <Col sm={9}>
                <Form.Control
                  type="text"
                  id="algorithmName"
                  name="algorithmName"
                  readOnly={!edit}
                  defaultValue={localComputation.algorithmName}
                  onChange={inputChange}
                />
              </Col>
            </FormGroup>
            <FormGroup as={Row} className="mb-3">
              <Form.Label column sm={3} htmlFor="applicationName">
                {t("computations:editor.applicationName")}
              </Form.Label>
              <Col sm={9}>
                <Form.Control
                  type="text"
                  id="applicationName"
                  name="applicationName"
                  readOnly={!edit}
                  defaultValue={localComputation.applicationName}
                  onChange={inputChange}
                />
              </Col>
            </FormGroup>
            <FormGroup as={Row} className="mb-3">
              <Form.Label column sm={3} htmlFor="groupName">
                {t("computations:editor.groupName")}
              </Form.Label>
              <Col sm={9}>
                <Form.Control
                  type="text"
                  id="groupName"
                  name="groupName"
                  readOnly={!edit}
                  defaultValue={localComputation.groupName}
                  onChange={inputChange}
                />
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
                  defaultChecked={localComputation.enabled ?? false}
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
                  defaultValue={localComputation.comment}
                  onChange={inputChange}
                />
              </Col>
            </FormGroup>
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
                onClick={() => saveComputation(localComputation)}
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
    </Card>
  );
};

export default Computation;
