import { useCallback, useMemo, useReducer } from "react";
import { Button, Card, Col, Form, FormGroup, Placeholder, Row } from "react-bootstrap";
import { Save, X } from "react-bootstrap-icons";
import { useTranslation } from "react-i18next";
import { ApiConfigSensorRecordingModeEnum, type ApiConfigSensor } from "opendcs-api";
import { DetailFade } from "../../../components/data-table";
import { PropertiesTable, type Property } from "../../../components/properties";
import type {
  CancelAction,
  CollectionActions,
  SaveAction,
} from "../../../util/Actions";

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };

const SENSOR_FIELDS = [
  "sensorNumber",
  "sensorName",
  "dataTypes",
  "recordingMode",
  "recordingInterval",
  "timeOfFirstSample",
  "absoluteMin",
  "absoluteMax",
  "usgsStatCode",
] as const;

export type UiConfigSensor = Partial<ApiConfigSensor>;

export interface ConfigSensorProperties {
  sensor: UiConfigSensor;
  edit?: boolean;
  actions?: SaveAction<ApiConfigSensor> & CancelAction<number>;
}

export const ConfigSensorSkeleton: React.FC<{
  edit?: boolean;
  className?: string;
}> = ({ edit = false, className }) => (
  <Card
    className={[
      "config-sensor-card",
      edit ? "config-sensor-card--edit" : null,
      className,
    ]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        <Col md={6}>
          {SENSOR_FIELDS.map((field) => (
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

type SensorReducerAction =
  | { type: "save"; payload: UiConfigSensor }
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } }
  | { type: "save_datatype"; payload: { standard: string; code: string } }
  | { type: "delete_datatype"; payload: { standard: string } };

function sensorReducer(
  current: UiConfigSensor,
  action: SensorReducerAction,
): UiConfigSensor {
  switch (action.type) {
    case "save":
      return { ...current, ...action.payload };
    case "save_prop":
      return {
        ...current,
        properties: {
          ...current.properties,
          [action.payload.name]: action.payload.value,
        },
      };
    case "delete_prop": {
      const next = { ...current.properties };
      delete next[action.payload.name];
      return { ...current, properties: next };
    }
    case "save_datatype":
      return {
        ...current,
        dataTypes: {
          ...current.dataTypes,
          [action.payload.standard]: action.payload.code,
        },
      };
    case "delete_datatype": {
      const next = { ...current.dataTypes };
      delete next[action.payload.standard];
      return { ...current, dataTypes: next };
    }
  }
}

const toNumberOrUndefined = (value: string): number | undefined => {
  if (value === "") return undefined;
  const n = Number(value);
  return Number.isNaN(n) ? undefined : n;
};

const mapProps = (props: { [k: string]: string } | undefined): Property[] =>
  Object.entries(props ?? {}).map(([name, value]): Property => ({ name, value }));

const mapDataTypes = (dt: { [k: string]: string } | undefined): Property[] =>
  Object.entries(dt ?? {}).map(([name, value]): Property => ({ name, value }));

export const ConfigSensor: React.FC<ConfigSensorProperties> = ({
  sensor,
  edit = false,
  actions = {},
}) => {
  const [t] = useTranslation(["configs", "translation"]);
  const [local, dispatch] = useReducer(sensorReducer, sensor);

  const props = useMemo(() => mapProps(local.properties), [local.properties]);
  const dataTypes = useMemo(() => mapDataTypes(local.dataTypes), [local.dataTypes]);

  const propertyActions: CollectionActions<Property, string> = edit
    ? {
        remove: (name) => dispatch({ type: "delete_prop", payload: { name } }),
        save: (prop) =>
          dispatch({
            type: "save_prop",
            payload: { name: prop.name, value: prop.value },
          }),
      }
    : {};

  const dataTypeActions: CollectionActions<Property, string> = edit
    ? {
        remove: (standard) =>
          dispatch({ type: "delete_datatype", payload: { standard } }),
        save: (entry) =>
          dispatch({
            type: "save_datatype",
            payload: { standard: entry.name, code: entry.value },
          }),
      }
    : {};

  const inputChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: value } });
  }, []);

  const numberChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: toNumberOrUndefined(value) } });
  }, []);

  const selectChange = useCallback((event: React.ChangeEvent<HTMLSelectElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: value || undefined } });
  }, []);

  const saveSensor = useCallback(() => {
    actions.save?.(local as ApiConfigSensor);
  }, [actions, local]);

  const cancel = useCallback(() => {
    if (sensor.sensorNumber !== undefined) actions.cancel?.(sensor.sensorNumber);
  }, [actions, sensor.sensorNumber]);

  return (
    <DetailFade skeleton={<ConfigSensorSkeleton edit={edit} />}>
      <Card
        className={["config-sensor-card", edit ? "config-sensor-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Row>
            <Col md={6}>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="sensorNumber">
                  {t("configs:sensor_num")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="number"
                    id="sensorNumber"
                    name="sensorNumber"
                    readOnly={!edit}
                    defaultValue={local.sensorNumber ?? ""}
                    onChange={numberChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="sensorName">
                  {t("configs:sensor_name")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="text"
                    id="sensorName"
                    name="sensorName"
                    readOnly={!edit}
                    defaultValue={local.sensorName ?? ""}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="recordingMode">
                  {t("configs:recording_mode")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Select
                    id="recordingMode"
                    name="recordingMode"
                    disabled={!edit}
                    value={local.recordingMode ?? ""}
                    onChange={selectChange}
                  >
                    <option value="" />
                    {Object.values(ApiConfigSensorRecordingModeEnum).map((mode) => (
                      <option key={mode} value={mode}>
                        {mode}
                      </option>
                    ))}
                  </Form.Select>
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="recordingInterval">
                  {t("configs:recording_interval")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="number"
                    id="recordingInterval"
                    name="recordingInterval"
                    readOnly={!edit}
                    defaultValue={local.recordingInterval ?? ""}
                    onChange={numberChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="timeOfFirstSample">
                  {t("configs:time_of_first_sample")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="number"
                    id="timeOfFirstSample"
                    name="timeOfFirstSample"
                    readOnly={!edit}
                    defaultValue={local.timeOfFirstSample ?? ""}
                    onChange={numberChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="absoluteMin">
                  {t("configs:absolute_min")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="number"
                    step="any"
                    id="absoluteMin"
                    name="absoluteMin"
                    readOnly={!edit}
                    defaultValue={local.absoluteMin ?? ""}
                    onChange={numberChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="absoluteMax">
                  {t("configs:absolute_max")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="number"
                    step="any"
                    id="absoluteMax"
                    name="absoluteMax"
                    readOnly={!edit}
                    defaultValue={local.absoluteMax ?? ""}
                    onChange={numberChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="usgsStatCode">
                  {t("configs:usgs_stat_code")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="text"
                    id="usgsStatCode"
                    name="usgsStatCode"
                    readOnly={!edit}
                    defaultValue={local.usgsStatCode ?? ""}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
            </Col>
            <Col md={6}>
              <PropertiesTable
                theProps={dataTypes}
                actions={dataTypeActions}
                edit={edit}
                canAdd={true}
                width={"100%"}
                height={"auto"}
                caption={t("configs:data_types")}
              />
              <div className="mt-4" />
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
                  onClick={cancel}
                  variant="secondary"
                  aria-label={t("configs:cancel_sensor_for", {
                    id: sensor.sensorNumber,
                  })}
                >
                  <X /> {t("translation:cancel")}
                </Button>
                <Button
                  onClick={saveSensor}
                  variant="primary"
                  aria-label={t("configs:save_sensor", {
                    id: sensor.sensorNumber,
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

export default ConfigSensor;
