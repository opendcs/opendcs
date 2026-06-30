import { useCallback, useMemo, useReducer, useState } from "react";
import {
  Button,
  Card,
  Col,
  Form,
  FormGroup,
  InputGroup,
  Placeholder,
  Row,
} from "react-bootstrap";
import { Save, X } from "react-bootstrap-icons";
import { useTranslation } from "react-i18next";
import type { ApiConfigSensor, ApiPlatformSensor, ApiSiteRef } from "opendcs-api";
import { DetailFade } from "../../components/data-table";
import { PropertiesTable, type Property } from "../../components/properties";
import type { CancelAction, CollectionActions } from "../../util/Actions";
import { PlatformSensorReducer } from "./PlatformSensorReducer";
import { SiteSelectModal } from "./SiteSelectModal";
import { siteDisplayName } from "./siteDisplayName";

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };

const CONFIG_FIELDS = [
  "sensorName",
  "dataTypes",
  "recordingMode",
  "recordingInterval",
  "absoluteMin",
  "absoluteMax",
] as const;

const OVERRIDE_FIELDS = ["actualSiteId", "min", "max", "usgsDdno"] as const;

export type UiPlatformSensor = Partial<ApiPlatformSensor>;

export const PlatformSensorSkeleton: React.FC<{
  edit?: boolean;
  className?: string;
}> = ({ edit = false, className }) => (
  <Card
    className={[
      "platform-sensor-card",
      edit ? "platform-sensor-card--edit" : null,
      className,
    ]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        <Col md={6}>
          {CONFIG_FIELDS.map((field) => (
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
          {OVERRIDE_FIELDS.map((field) => (
            <Row key={field} className="mb-3 align-items-center">
              <Placeholder as={Col} sm={4} animation="glow">
                <Placeholder xs={10} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} sm={8} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
          ))}
          <Placeholder animation="glow" className="d-block">
            <Placeholder xs={12} style={{ height: "8rem" }} />
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

export interface PlatformSensorProperties {
  configSensor: ApiConfigSensor;
  override: UiPlatformSensor;
  actions?: {
    save?: (sensor: ApiPlatformSensor) => void;
  } & CancelAction<number>;
  edit?: boolean;
}

const mapProps: (props: { [k: string]: string }) => Property[] = (props) =>
  Object.entries(props).map(([name, value]): Property => ({ name, value }));

const toNumberOrUndefined = (value: string): number | undefined => {
  if (value === "") return undefined;
  const n = Number(value);
  return Number.isNaN(n) ? undefined : n;
};

const formatDataTypes = (dt?: { [k: string]: string }): string => {
  if (!dt) return "";
  return Object.entries(dt)
    .map(([std, code]) => `${std}: ${code}`)
    .join(", ");
};

export const PlatformSensor: React.FC<PlatformSensorProperties> = ({
  configSensor,
  override,
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["platforms", "translation"]);
  const [local, dispatch] = useReducer(PlatformSensorReducer, override);
  const [showSiteModal, setShowSiteModal] = useState(false);
  const [siteName, setSiteName] = useState<string>(
    local.actualSiteId === undefined ? "" : String(local.actualSiteId),
  );

  const handleSelectSite = useCallback((ref: ApiSiteRef) => {
    if (ref.siteId === undefined) return;
    dispatch({ type: "save", payload: { actualSiteId: ref.siteId } });
    setSiteName(siteDisplayName(ref));
  }, []);

  const clearSite = useCallback(() => {
    dispatch({ type: "save", payload: { actualSiteId: undefined } });
    setSiteName("");
  }, []);

  const props = useMemo(() => mapProps(local.sensorProps || {}), [local.sensorProps]);

  const propertyActions: CollectionActions<Property, string> = edit
    ? {
        remove: (propName) =>
          dispatch({ type: "delete_prop", payload: { name: propName } }),
        save: (prop) =>
          dispatch({
            type: "save_prop",
            payload: { name: prop.name, value: prop.value },
          }),
      }
    : {};

  const numberChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({
      type: "save",
      payload: { [name]: toNumberOrUndefined(value) },
    });
  }, []);

  const saveSensor = useCallback(() => {
    const payload: ApiPlatformSensor = {
      ...local,
      sensorNum: configSensor.sensorNumber,
    } as ApiPlatformSensor;
    actions.save?.(payload);
  }, [actions, local, configSensor.sensorNumber]);

  const cancel = useCallback(() => {
    if (configSensor.sensorNumber !== undefined)
      actions.cancel?.(configSensor.sensorNumber);
  }, [actions, configSensor.sensorNumber]);

  return (
    <DetailFade skeleton={<PlatformSensorSkeleton edit={edit} />}>
      <Card
        className={["platform-sensor-card", edit ? "platform-sensor-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Row>
            <Col md={6}>
              <h6 className="text-muted text-uppercase mb-3">
                {t("platforms:from_config")}
              </h6>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4}>
                  {t("platforms:sensor_num")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    plaintext
                    readOnly
                    value={configSensor.sensorNumber ?? ""}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4}>
                  {t("platforms:sensor_name")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    plaintext
                    readOnly
                    value={configSensor.sensorName ?? ""}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4}>
                  {t("platforms:data_types")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    plaintext
                    readOnly
                    value={formatDataTypes(configSensor.dataTypes)}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4}>
                  {t("platforms:recording_mode")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    plaintext
                    readOnly
                    value={configSensor.recordingMode ?? ""}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4}>
                  {t("platforms:recording_interval")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    plaintext
                    readOnly
                    value={configSensor.recordingInterval ?? ""}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4}>
                  {t("platforms:absolute_min")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    plaintext
                    readOnly
                    value={configSensor.absoluteMin ?? ""}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4}>
                  {t("platforms:absolute_max")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    plaintext
                    readOnly
                    value={configSensor.absoluteMax ?? ""}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4}>
                  {t("platforms:usgs_stat_code")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    plaintext
                    readOnly
                    value={configSensor.usgsStatCode ?? ""}
                  />
                </Col>
              </FormGroup>
            </Col>
            <Col md={6}>
              <h6 className="text-muted text-uppercase mb-3">
                {t("platforms:platform_overrides")}
              </h6>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="actualSiteId">
                  {t("platforms:actual_site_id")}
                </Form.Label>
                <Col sm={8}>
                  {edit ? (
                    <div className="d-flex gap-2">
                      <InputGroup>
                        <Form.Control
                          type="text"
                          id="actualSiteId"
                          name="actualSiteId"
                          readOnly={true}
                          disabled={true}
                          value={siteName}
                          placeholder={t("platforms:same_as_platform_site")}
                        />
                        <Button
                          variant="secondary"
                          className="dt-button"
                          aria-label={t("platforms:select_site")}
                          onClick={() => setShowSiteModal(true)}
                        >
                          {t("translation:choose")}
                        </Button>
                      </InputGroup>
                      <Button
                        variant="outline-secondary"
                        className="dt-button"
                        style={{ paddingLeft: "0.4rem", paddingRight: "0.4rem" }}
                        aria-label={t("platforms:clear_actual_site")}
                        disabled={local.actualSiteId === undefined}
                        onClick={clearSite}
                      >
                        <X size={18} />
                      </Button>
                    </div>
                  ) : (
                    <Form.Control
                      type="text"
                      id="actualSiteId"
                      readOnly={true}
                      value={siteName}
                      placeholder={t("platforms:same_as_platform_site")}
                    />
                  )}
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="min">
                  {t("platforms:sensor_min")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="number"
                    step="any"
                    id="min"
                    name="min"
                    readOnly={!edit}
                    defaultValue={local.min ?? ""}
                    onChange={numberChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="max">
                  {t("platforms:sensor_max")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="number"
                    step="any"
                    id="max"
                    name="max"
                    readOnly={!edit}
                    defaultValue={local.max ?? ""}
                    onChange={numberChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="usgsDdno">
                  {t("platforms:usgs_ddno")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="number"
                    id="usgsDdno"
                    name="usgsDdno"
                    readOnly={!edit}
                    defaultValue={local.usgsDdno ?? ""}
                    onChange={numberChange}
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
                  onClick={cancel}
                  variant="secondary"
                  aria-label={t("platforms:cancel_sensor_for", {
                    id: configSensor.sensorNumber,
                  })}
                >
                  <X /> {t("translation:cancel")}
                </Button>
                <Button
                  onClick={saveSensor}
                  variant="primary"
                  aria-label={t("platforms:save_sensor", {
                    id: configSensor.sensorNumber,
                  })}
                >
                  <Save /> {t("translation:save")}
                </Button>
              </Col>
            </Row>
          )}
        </Card.Body>
      </Card>
      <SiteSelectModal
        show={showSiteModal}
        onHide={() => setShowSiteModal(false)}
        onSelect={handleSelectSite}
      />
    </DetailFade>
  );
};

export default PlatformSensor;
