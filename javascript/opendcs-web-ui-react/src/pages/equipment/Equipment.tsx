import { Button, Card, Col, Form, FormGroup, Placeholder, Row } from "react-bootstrap";
import { PropertiesTable, type Property } from "../../components/properties";
import { use, useCallback, useMemo, useReducer } from "react";
import type { ApiEquipmentModel } from "opendcs-api";
import { useTranslation } from "react-i18next";
import type { CancelAction, CollectionActions, SaveAction } from "../../util/Actions";
import { Save, X } from "react-bootstrap-icons";
import { EquipmentReducer } from "./EquipmentReducer";
import { DetailFade } from "../../components/data-table";

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };

const EQUIPMENT_FIELDS = [
  "name",
  "equipmentType",
  "company",
  "model",
  "description",
] as const;

export type UiEquipmentModel = Partial<ApiEquipmentModel>;

export interface EquipmentSkeletonProps {
  edit?: boolean;
  className?: string;
}

export const EquipmentSkeleton: React.FC<EquipmentSkeletonProps> = ({
  edit = false,
  className,
}) => (
  <Card className={["equipment-card", className].filter(Boolean).join(" ")}>
    <Card.Body>
      <Row>
        <Col>
          {EQUIPMENT_FIELDS.map((f) => (
            <Row key={f} className="mb-3 align-items-center">
              <Placeholder as={Col} sm={3} animation="glow">
                <Placeholder xs={8} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} sm={9} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
          ))}
        </Col>
        <Col>
          <Placeholder animation="glow">
            <Placeholder xs={12} className="rounded" style={{ height: "10rem" }} />
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

export interface EquipmentProperties {
  equipment: Promise<UiEquipmentModel> | UiEquipmentModel;
  actions?: SaveAction<ApiEquipmentModel> & CancelAction<number>;
  edit?: boolean;
}

export const Equipment: React.FC<EquipmentProperties> = ({
  equipment,
  actions = {},
  edit = false,
}) => {
  const { t } = useTranslation(["equipment", "translation"]);
  const provided = equipment instanceof Promise ? use(equipment) : equipment;
  const [local, dispatch] = useReducer(EquipmentReducer, provided);

  const props = useMemo<Property[]>(
    () =>
      Object.entries(local.properties ?? {}).map(([name, value]) => ({
        name,
        value,
      })),
    [local.properties],
  );

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

  const save = useCallback(() => {
    actions.save?.(local as ApiEquipmentModel);
  }, [actions, local]);

  const inputChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      const { name, value } = event.target;
      dispatch({ type: "save", payload: { [name]: value } });
    },
    [],
  );

  return (
    <DetailFade skeleton={<EquipmentSkeleton edit={edit} />}>
      <Card className="equipment-card">
        <Card.Body>
          <Row>
            <Col>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="name">
                  {t("equipment:name")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="text"
                    id="name"
                    name="name"
                    readOnly={!edit}
                    maxLength={64}
                    defaultValue={local.name}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>

              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="equipmentType">
                  {t("equipment:equipment_type")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="text"
                    id="equipmentType"
                    name="equipmentType"
                    readOnly={!edit}
                    defaultValue={local.equipmentType}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>

              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="company">
                  {t("equipment:company")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="text"
                    id="company"
                    name="company"
                    readOnly={!edit}
                    defaultValue={local.company ?? ""}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>

              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="model">
                  {t("equipment:model")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="text"
                    id="model"
                    name="model"
                    readOnly={!edit}
                    defaultValue={local.model ?? ""}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>

              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="description">
                  {t("equipment:description")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    as="textarea"
                    rows={3}
                    id="description"
                    name="description"
                    readOnly={!edit}
                    defaultValue={local.description ?? ""}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
            </Col>

            <Col>
              <PropertiesTable
                theProps={props}
                actions={propertyActions}
                edit={edit}
                canAdd={true}
                width="100%"
                height="auto"
              />
            </Col>
          </Row>

          {edit && (
            <Row className="mt-3">
              <Col className="d-flex justify-content-end gap-2">
                <Button
                  onClick={() =>
                    provided.equipmentId !== undefined &&
                    actions.cancel?.(provided.equipmentId)
                  }
                  variant="secondary"
                  aria-label={t("equipment:cancel_for", { id: provided.equipmentId })}
                >
                  <X /> {t("translation:cancel")}
                </Button>
                <Button
                  onClick={save}
                  variant="primary"
                  aria-label={t("equipment:save_equipment", { id: local.equipmentId })}
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

export default Equipment;
