import { use, useCallback, useReducer } from "react";
import { Button, Card, Col, Form, FormGroup, Placeholder, Row } from "react-bootstrap";
import { Save, X } from "react-bootstrap-icons";
import { useTranslation } from "react-i18next";
import type { ApiDecodedMessage, ApiPlatformConfig } from "opendcs-api";
import { DetailFade } from "../../../components/data-table";
import type { CancelAction, SaveAction } from "../../../util/Actions";
import { ConfigReducer, type UiConfig } from "./ConfigReducer";
import ConfigSensorsTable from "./ConfigSensorsTable";
import ConfigScriptsTable from "./ConfigScriptsTable";

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };

const CONFIG_FIELDS = ["name", "numPlatforms", "description"] as const;

const EMPTY_DECODED: ApiDecodedMessage = {} as ApiDecodedMessage;

const TablePlaceholder: React.FC = () => (
  <>
    <Placeholder animation="glow" className="d-block mb-2">
      <Placeholder xs={12} className="rounded" style={{ height: "2rem" }} />
    </Placeholder>
    <Placeholder animation="glow" className="d-block">
      <Placeholder xs={12} className="rounded" style={{ height: "8rem" }} />
    </Placeholder>
  </>
);

export const ConfigSkeleton: React.FC<{ edit?: boolean; className?: string }> = ({
  edit = false,
  className,
}) => (
  <Card
    className={["config-card", edit ? "config-card--edit" : null, className]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      {CONFIG_FIELDS.map((field) => (
        <Row key={field} className="mb-3 align-items-center">
          <Placeholder as={Col} sm={3} animation="glow">
            <Placeholder xs={10} className="rounded" style={LABEL_H} />
          </Placeholder>
          <Placeholder as={Col} sm={9} animation="glow">
            <Placeholder xs={12} className="rounded" style={INPUT_H} />
          </Placeholder>
        </Row>
      ))}
      <Row className="mt-4">
        <Col md={12}>
          <TablePlaceholder />
        </Col>
      </Row>
      <Row className="mt-4">
        <Col md={12}>
          <TablePlaceholder />
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

export interface ConfigDetails {
  config: UiConfig;
}

export interface ConfigProperties {
  details: Promise<ConfigDetails> | ConfigDetails;
  actions?: SaveAction<ApiPlatformConfig> & CancelAction<number>;
  edit?: boolean;
  /**
   * Imperative decode invocation for the embedded sample tester in
   * DecodesScriptEditor. Lifted to the page so it can be wired to the API.
   */
  decodeData?: (raw: string) => ApiDecodedMessage;
}

export const Config: React.FC<ConfigProperties> = ({
  details,
  actions = {},
  edit = false,
  decodeData,
}) => {
  const [t] = useTranslation(["configs", "translation"]);
  const resolved = details instanceof Promise ? use(details) : details;
  const provided = resolved.config;
  const [local, dispatch] = useReducer(ConfigReducer, provided);

  const inputChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: value } });
  }, []);

  const saveConfig = useCallback(() => {
    actions.save?.(local as ApiPlatformConfig);
  }, [actions, local]);

  const cancel = useCallback(() => {
    if (provided.configId !== undefined) actions.cancel?.(provided.configId);
  }, [actions, provided.configId]);

  const noopDecode = useCallback(
    (_raw: string): ApiDecodedMessage => EMPTY_DECODED,
    [],
  );

  return (
    <DetailFade skeleton={<ConfigSkeleton edit={edit} />}>
      <Card
        className={["config-card", edit ? "config-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Row>
            <Col md={6}>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="name">
                  {t("configs:name")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="text"
                    id="name"
                    name="name"
                    readOnly={!edit}
                    defaultValue={local.name ?? ""}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="numPlatforms">
                  {t("configs:numPlatforms")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    plaintext
                    readOnly
                    id="numPlatforms"
                    value={local.numPlatforms ?? 0}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="description">
                  {t("configs:description")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    as="textarea"
                    rows={3}
                    id="description"
                    name="description"
                    readOnly={!edit}
                    defaultValue={local.description ?? ""}
                    onChange={
                      inputChange as unknown as React.ChangeEventHandler<HTMLTextAreaElement>
                    }
                  />
                </Col>
              </FormGroup>
            </Col>
          </Row>
          <Row className="mt-4">
            <Col md={12}>
              <ConfigSensorsTable
                sensors={local.configSensors ?? []}
                edit={edit}
                actions={{
                  save: (sensor, originalSensorNumber) =>
                    dispatch({
                      type: "save_sensor",
                      payload: { sensor, originalSensorNumber },
                    }),
                  remove: (sensorNumber) =>
                    dispatch({
                      type: "delete_sensor",
                      payload: { sensorNumber },
                    }),
                }}
              />
            </Col>
          </Row>
          <Row className="mt-4">
            <Col md={12}>
              <ConfigScriptsTable
                scripts={local.scripts ?? []}
                configSensors={local.configSensors ?? []}
                decodeData={decodeData ?? noopDecode}
                edit={edit}
                actions={{
                  save: (script, originalName) =>
                    dispatch({
                      type: "save_script",
                      payload: { script, originalName },
                    }),
                  remove: (name) =>
                    dispatch({ type: "delete_script", payload: { name } }),
                }}
              />
            </Col>
          </Row>
          {edit && (
            <Row className="mt-3">
              <Col className="d-flex justify-content-end gap-2">
                <Button
                  onClick={cancel}
                  variant="secondary"
                  aria-label={t("configs:cancel_for", { id: provided.configId })}
                >
                  <X /> {t("translation:cancel")}
                </Button>
                <Button
                  onClick={saveConfig}
                  variant="primary"
                  aria-label={t("configs:save_config", { id: provided.configId })}
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

export default Config;
