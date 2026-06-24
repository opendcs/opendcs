import { use, useCallback, useMemo, useReducer } from "react";
import { Card, Col, Form, FormGroup, Placeholder, Row } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiPresentationElement, ApiPresentationGroup } from "opendcs-api";
import { DetailFade } from "../../../components/data-table";
import {
  CancelButton,
  EditFormActions,
  INPUT_H,
  LABEL_H,
  SaveButton,
} from "../../../components/forms";
import type { CancelAction, SaveAction } from "../../../util/Actions";
import { usePresentationRefsQuery } from "../../../queries/presentations";
import { useDataTypeListQuery } from "../../../queries/dataTypes";
import { useUnitListQuery } from "../../../queries/units";
import {
  PresentationReducer,
  elementKey,
  type UiPresentation,
} from "./PresentationReducer";
import PresentationElementsTable from "./PresentationElementsTable";

const PRESENTATION_FIELDS = ["name", "inheritsFrom", "production"] as const;

export interface PresentationSkeletonProps {
  edit?: boolean;
  className?: string;
}

export const PresentationSkeleton: React.FC<PresentationSkeletonProps> = ({
  edit = false,
  className,
}) => (
  <Card
    className={["presentation-card", edit ? "presentation-card--edit" : null, className]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        <Col md={6}>
          {PRESENTATION_FIELDS.map((field) => (
            <Row key={field} className="mb-3 align-items-center">
              <Placeholder as={Col} sm={4} animation="glow">
                <Placeholder xs={10} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} sm={8} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
          ))}
        </Col>
        <Col md={6}>
          <Placeholder animation="glow" className="d-block">
            <Placeholder xs={12} style={{ height: "16rem" }} />
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

export interface PresentationDetails {
  presentation: UiPresentation;
}

export interface PresentationProperties {
  details: Promise<PresentationDetails> | PresentationDetails;
  actions?: SaveAction<ApiPresentationGroup> & CancelAction<number>;
  edit?: boolean;
}

export const Presentation: React.FC<PresentationProperties> = ({
  details,
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["presentations", "translation"]);
  const resolved = details instanceof Promise ? use(details) : details;
  const provided = resolved.presentation;
  const [local, dispatch] = useReducer(PresentationReducer, provided);

  const { data: groups = [] } = usePresentationRefsQuery();
  // Prefetch so dropdowns in PresentationElementsTable render immediately.
  useDataTypeListQuery();
  useUnitListQuery();
  const parentOptions = useMemo(
    () =>
      groups
        .map((g) => g.name ?? "")
        .filter(Boolean)
        // A group cannot inherit from itself.
        .filter((name) => name !== local.name)
        .sort((a, b) => a.localeCompare(b)),
    [groups, local.name],
  );

  const elementsList = useMemo(() => local.elements ?? [], [local.elements]);

  const textChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: value } });
  }, []);

  const setField = useCallback(
    <K extends keyof UiPresentation>(name: K, value: UiPresentation[K]) =>
      dispatch({ type: "save", payload: { [name]: value } as UiPresentation }),
    [],
  );

  const onElementSave = useCallback(
    (original: ApiPresentationElement, updated: ApiPresentationElement) => {
      const originalKey =
        original.dataTypeStd || original.dataTypeCode
          ? elementKey(original)
          : undefined;
      dispatch({
        type: "save_element",
        payload: { originalKey, element: updated },
      });
    },
    [],
  );

  const onElementRemove = useCallback(
    (key: string) => dispatch({ type: "delete_element", payload: { key } }),
    [],
  );

  const onProductionChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) =>
      setField("production", event.target.checked),
    [setField],
  );

  const onParentChange = useCallback(
    (event: React.ChangeEvent<HTMLSelectElement>) => {
      const name = event.currentTarget.value;
      const ref = groups.find((g) => g.name === name);
      dispatch({
        type: "save",
        payload: {
          inheritsFrom: name || undefined,
          inheritsFromId: ref?.groupId,
        },
      });
    },
    [groups],
  );

  const savePresentation = useCallback(() => {
    actions.save?.(local as ApiPresentationGroup);
  }, [actions, local]);

  const cancel = useCallback(() => {
    if (provided.groupId !== undefined) actions.cancel?.(provided.groupId);
  }, [actions, provided.groupId]);

  return (
    <DetailFade skeleton={<PresentationSkeleton edit={edit} />}>
      <Card
        className={["presentation-card", edit ? "presentation-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Row>
            <Col md={6}>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="name">
                  {t("presentations:name")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="text"
                    id="name"
                    name="name"
                    readOnly={!edit}
                    defaultValue={local.name ?? ""}
                    onChange={textChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="inheritsFrom">
                  {t("presentations:inheritsFrom")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Select
                    id="inheritsFrom"
                    aria-label={t("presentations:inheritsFrom")}
                    value={local.inheritsFrom ?? ""}
                    disabled={!edit}
                    onChange={onParentChange}
                  >
                    <option value="" />
                    {parentOptions.map((name) => (
                      <option key={name} value={name}>
                        {name}
                      </option>
                    ))}
                  </Form.Select>
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3 align-items-center">
                <Form.Label column sm={4} htmlFor="production">
                  {t("presentations:production")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Check
                    id="production"
                    name="production"
                    disabled={!edit}
                    checked={local.production ?? false}
                    onChange={onProductionChange}
                  />
                </Col>
              </FormGroup>
            </Col>
            <Col md={6}>
              <PresentationElementsTable
                elements={elementsList}
                edit={edit}
                onSave={onElementSave}
                onRemove={onElementRemove}
              />
            </Col>
          </Row>

          {edit && (
            <EditFormActions>
              <CancelButton
                onClick={cancel}
                aria-label={t("presentations:cancel_for", {
                  id: provided.groupId,
                })}
              />
              <SaveButton
                onClick={savePresentation}
                aria-label={t("presentations:save_presentation", {
                  id: provided.groupId,
                })}
              />
            </EditFormActions>
          )}
        </Card.Body>
      </Card>
    </DetailFade>
  );
};

export default Presentation;
