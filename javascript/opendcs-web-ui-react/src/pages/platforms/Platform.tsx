import {
  Button,
  Card,
  Col,
  Form,
  FormCheck,
  FormGroup,
  InputGroup,
  Placeholder,
  Row,
} from "react-bootstrap";
import { PropertiesTable, type Property } from "../../components/properties";
import { use, useCallback, useMemo, useReducer, useState } from "react";
import type {
  ApiConfigRef,
  ApiConfigSensor,
  ApiPlatform,
  ApiPlatformConfig,
  ApiSite,
  ApiSiteRef,
} from "opendcs-api";
import { useTranslation } from "react-i18next";
import type { CancelAction, CollectionActions, SaveAction } from "../../util/Actions";
import { Save, X } from "react-bootstrap-icons";
import { DetailFade } from "../../components/data-table";
import { useFetchConfig } from "../../queries/configs";
import { PlatformReducer } from "./PlatformReducer";
import { SiteSelectModal } from "./SiteSelectModal";
import { ConfigSelectModal } from "./ConfigSelectModal";
import { PlatformSensorsTable } from "./PlatformSensorsTable";
import { TransportMediaTable } from "./TransportMediaTable";
import { siteDisplayName } from "./siteDisplayName";

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };

const PLATFORM_FIELDS = [
  "name",
  "agency",
  "designator",
  "description",
  "siteId",
  "configId",
  "production",
] as const;

export type UiPlatform = Partial<ApiPlatform>;

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

export const PlatformSkeleton: React.FC<{ edit?: boolean; className?: string }> = ({
  edit = false,
  className,
}) => (
  <Card
    className={["platform-card", edit ? "platform-card--edit" : null, className]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        <Col>
          {PLATFORM_FIELDS.map((field) => (
            <Row key={field} className="mb-3 align-items-center">
              <Placeholder as={Col} sm={3} animation="glow">
                <Placeholder xs={10} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} sm={9} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
          ))}
        </Col>
        <Col>
          <Placeholder animation="glow" className="d-block">
            <Placeholder xs={12} style={{ height: "12rem" }} />
          </Placeholder>
        </Col>
      </Row>
      <Row className="mt-4">
        <Col md={6}>
          <TablePlaceholder />
        </Col>
        <Col md={6}>
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

export interface PlatformDetails {
  platform: UiPlatform;
  site?: ApiSite;
  config?: ApiPlatformConfig;
}

export interface PlatformProperties {
  details: Promise<PlatformDetails> | PlatformDetails;
  actions?: SaveAction<ApiPlatform> & CancelAction<number>;
  edit?: boolean;
  /**
   * Invoked when the user clicks the "edit site" button. Lifted out of the
   * component because `Platform` is rendered into a detached React root for
   * the DataTable child row, where router hooks like `useNavigate` don't work.
   */
  onEditSite?: (siteId: number) => void;
}

const mapProps: (props: { [k: string]: string }) => Property[] = (props) =>
  Object.entries(props).map(([name, value]): Property => ({ name, value }));

export const Platform: React.FC<PlatformProperties> = ({
  details,
  actions = {},
  edit = false,
  onEditSite,
}) => {
  const [t] = useTranslation(["platforms", "translation"]);
  const resolved = details instanceof Promise ? use(details) : details;
  const providedPlatform = resolved.platform;
  const [localPlatform, dispatch] = useReducer(PlatformReducer, providedPlatform);

  const props = useMemo(
    () => mapProps(localPlatform.properties || {}),
    [localPlatform.properties],
  );

  // Initially seeded from the resolved record; updated locally when the user
  // picks a different site/config via the chooser modals so the input field
  // reflects the new choice without another single-record fetch.
  const [siteName, setSiteName] = useState<string>(
    resolved.site ? siteDisplayName(resolved.site) : "",
  );
  const [configName, setConfigName] = useState<string>(resolved.config?.name ?? "");
  // Mirrors configName: seeded from the resolved record, then refreshed when
  // the user picks a different config so the Sensors table reflects it
  // instead of staying pinned to whatever config (if any) the platform
  // originally resolved with.
  const [configSensors, setConfigSensors] = useState<ApiConfigSensor[]>(
    resolved.config?.configSensors ?? [],
  );
  const [showSiteModal, setShowSiteModal] = useState(false);
  const [showConfigModal, setShowConfigModal] = useState(false);
  const fetchConfig = useFetchConfig();

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

  const savePlatform = useCallback(() => {
    actions.save?.(localPlatform as ApiPlatform);
  }, [actions, localPlatform]);

  const inputChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: value } });
  }, []);

  const checkboxChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, checked } = event.target;
    dispatch({ type: "save", payload: { [name]: checked } });
  }, []);

  const handleSelectSite = useCallback((ref: ApiSiteRef) => {
    if (ref.siteId === undefined) return;
    dispatch({ type: "save", payload: { siteId: ref.siteId } });
    setSiteName(siteDisplayName(ref));
  }, []);

  const handleSelectConfig = useCallback(
    (ref: ApiConfigRef) => {
      if (ref.configId === undefined) return;
      dispatch({ type: "save", payload: { configId: ref.configId } });
      setConfigName(ref.name ?? String(ref.configId));
      fetchConfig(ref.configId)
        .then((config) => setConfigSensors(config.configSensors ?? []))
        .catch(() => setConfigSensors([]));
    },
    [fetchConfig],
  );

  return (
    <DetailFade skeleton={<PlatformSkeleton edit={edit} />}>
      <Card
        className={["platform-card", edit ? "platform-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Row>
            <Col>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="name">
                  {t("platforms:site")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="text"
                    id="name"
                    name="name"
                    readOnly={true}
                    disabled={true}
                    defaultValue={localPlatform.name}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="siteId">
                  {t("platforms:public_name")}
                </Form.Label>
                <Col sm={9}>
                  {edit ? (
                    <div className="d-flex gap-2">
                      <InputGroup>
                        <Form.Control
                          type="text"
                          id="siteId"
                          name="siteId"
                          readOnly={true}
                          disabled={true}
                          value={siteName}
                          onChange={inputChange}
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
                      <button
                        type="button"
                        className="dt-button btn btn-secondary"
                        aria-label={t("platforms:edit_site")}
                        disabled={localPlatform.siteId === undefined || !onEditSite}
                        onClick={() => {
                          if (localPlatform.siteId !== undefined) {
                            onEditSite?.(localPlatform.siteId);
                          }
                        }}
                      >
                        edit
                      </button>
                    </div>
                  ) : (
                    <Form.Control
                      type="text"
                      id="siteId"
                      name="siteId"
                      readOnly={true}
                      value={siteName}
                      onChange={inputChange}
                    />
                  )}
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="designator">
                  {t("platforms:designator")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="text"
                    id="designator"
                    name="designator"
                    readOnly={!edit}
                    defaultValue={localPlatform.designator}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="configId">
                  {t("platforms:config")}
                </Form.Label>
                <Col sm={9}>
                  {edit ? (
                    <InputGroup>
                      <Form.Control
                        type="text"
                        id="configId"
                        name="configId"
                        readOnly={true}
                        disabled={true}
                        value={configName}
                        onChange={inputChange}
                      />
                      <Button
                        variant="secondary"
                        className="dt-button"
                        aria-label={t("platforms:select_config")}
                        onClick={() => setShowConfigModal(true)}
                      >
                        {t("translation:choose")}
                      </Button>
                    </InputGroup>
                  ) : (
                    <Form.Control
                      type="text"
                      id="configId"
                      name="configId"
                      readOnly={true}
                      value={configName}
                      onChange={inputChange}
                    />
                  )}
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="agency">
                  {t("platforms:agency")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="text"
                    id="agency"
                    name="agency"
                    readOnly={!edit}
                    defaultValue={localPlatform.agency}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="description">
                  {t("platforms:description")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="text"
                    id="description"
                    name="description"
                    readOnly={!edit}
                    defaultValue={localPlatform.description}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="production">
                  {t("platforms:production")}
                </Form.Label>
                <Col sm={9} className="d-flex align-items-center">
                  <FormCheck
                    type="switch"
                    id="production"
                    name="production"
                    disabled={!edit}
                    defaultChecked={localPlatform.production ?? false}
                    onChange={checkboxChange}
                    style={{ fontSize: "1.5rem" }}
                  />
                </Col>
              </FormGroup>
              {localPlatform.lastModified && (
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={3} htmlFor="lastModified">
                    {t("platforms:lastModified")}
                  </Form.Label>
                  <Col sm={9}>
                    <Form.Control
                      plaintext
                      readOnly
                      id="lastModified"
                      value={new Date(localPlatform.lastModified).toLocaleString()}
                    />
                  </Col>
                </FormGroup>
              )}
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
          <Row className="mt-4">
            <Col md={6} style={edit ? { paddingTop: "2.25rem" } : undefined}>
              <PlatformSensorsTable
                configSensors={configSensors}
                platformSensors={localPlatform.platformSensors ?? []}
                edit={edit}
                actions={{
                  save: (sensor, originalSensorNum) =>
                    dispatch({
                      type: "save_sensor",
                      payload: { sensor, originalSensorNum },
                    }),
                  remove: (sensorNum) =>
                    dispatch({
                      type: "delete_sensor",
                      payload: { sensorNum },
                    }),
                }}
              />
            </Col>
            <Col md={6}>
              <TransportMediaTable
                media={localPlatform.transportMedia ?? []}
                edit={edit}
                actions={{
                  save: (medium, originalKey) =>
                    dispatch({
                      type: "save_transport",
                      payload: { medium, originalKey },
                    }),
                  remove: (key) =>
                    dispatch({
                      type: "delete_transport",
                      payload: { key },
                    }),
                }}
              />
            </Col>
          </Row>
          {edit && (
            <Row className="mt-3">
              <Col className="d-flex justify-content-end gap-2">
                <Button
                  onClick={() => {
                    if (providedPlatform.platformId !== undefined) {
                      actions.cancel?.(providedPlatform.platformId);
                    }
                  }}
                  variant="secondary"
                  aria-label={t("platforms:cancel_for", {
                    id: providedPlatform.platformId,
                  })}
                >
                  <X /> {t("translation:cancel")}
                </Button>
                <Button
                  onClick={savePlatform}
                  variant="primary"
                  aria-label={t("platforms:save_platform", {
                    id: providedPlatform.platformId,
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
      <ConfigSelectModal
        show={showConfigModal}
        onHide={() => setShowConfigModal(false)}
        onSelect={handleSelectConfig}
      />
    </DetailFade>
  );
};

export default Platform;
