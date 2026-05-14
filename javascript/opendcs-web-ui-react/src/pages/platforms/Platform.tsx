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
import { use, useCallback, useMemo, useReducer } from "react";
import type { ApiPlatform, ApiPlatformConfig, ApiSite } from "opendcs-api";
import { useTranslation } from "react-i18next";
import type { CancelAction, CollectionActions, SaveAction } from "../../util/Actions";
import { Save, X } from "react-bootstrap-icons";
import { DetailFade } from "../../components/data-table";
import { PlatformReducer } from "./PlatformReducer";

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

const siteDisplayName = (site: ApiSite): string =>
  site.publicName ||
  (site.sitenames ? Object.values(site.sitenames)[0] : undefined) ||
  String(site.siteId ?? "");

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

  // Names from the initial fetch. If the user edits siteId/configId, this stays
  // pinned to the originally-fetched record — re-resolution would require
  // another single-record fetch and is out of scope here.
  const siteName = resolved.site ? siteDisplayName(resolved.site) : "";
  const configName = resolved.config?.name ?? "";

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
                  {t("platforms:name")}
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
                  {t("platforms:site")}
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
                          defaultValue={siteName}
                          onChange={inputChange}
                        />
                        <button
                          type="button"
                          className="dt-button btn btn-secondary"
                          aria-label={t("platforms:select_site")}
                        >
                          choose
                        </button>
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
                      defaultValue={siteName}
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
                        defaultValue={siteName}
                        onChange={inputChange}
                      />
                      <button
                        type="button"
                        className="dt-button btn btn-secondary"
                        aria-label={t("platforms:select_config")}
                      >
                        choose
                      </button>
                    </InputGroup>
                  ) : (
                    <Form.Control
                      type="text"
                      id="configId"
                      name="configId"
                      readOnly={true}
                      defaultValue={configName}
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
    </DetailFade>
  );
};

export default Platform;
