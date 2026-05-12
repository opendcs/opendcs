import {
  Button,
  Card,
  Col,
  Form,
  FormCheck,
  FormGroup,
  Placeholder,
  Row,
} from "react-bootstrap";
import { PropertiesTable, type Property } from "../../components/properties";
import { use, useCallback, useMemo, useReducer } from "react";
import type { ApiPlatform } from "opendcs-api";
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

export interface PlatformProperties {
  platform: Promise<UiPlatform> | UiPlatform;
  actions?: SaveAction<ApiPlatform> & CancelAction<number>;
  edit?: boolean;
}

const mapProps: (props: { [k: string]: string }) => Property[] = (props) =>
  Object.entries(props).map(([name, value]): Property => ({ name, value }));

export const Platform: React.FC<PlatformProperties> = ({
  platform,
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["platforms", "translation"]);
  const providedPlatform = platform instanceof Promise ? use(platform) : platform;
  const [localPlatform, dispatch] = useReducer(PlatformReducer, providedPlatform);

  const props = useMemo(
    () => mapProps(localPlatform.properties || {}),
    [localPlatform.properties],
  );

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

  const numberChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    const parsed = value === "" ? undefined : Number(value);
    dispatch({ type: "save", payload: { [name]: parsed } });
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
                    readOnly={!edit}
                    defaultValue={localPlatform.name}
                    onChange={inputChange}
                  />
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
                <Form.Label column sm={3} htmlFor="siteId">
                  {t("platforms:siteId")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="number"
                    id="siteId"
                    name="siteId"
                    readOnly={!edit}
                    defaultValue={localPlatform.siteId}
                    onChange={numberChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="configId">
                  {t("platforms:configId")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="number"
                    id="configId"
                    name="configId"
                    readOnly={!edit}
                    defaultValue={localPlatform.configId}
                    onChange={numberChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="production">
                  {t("platforms:production")}
                </Form.Label>
                <Col sm={9}>
                  <FormCheck
                    type="checkbox"
                    id="production"
                    name="production"
                    disabled={!edit}
                    defaultChecked={localPlatform.production ?? false}
                    onChange={checkboxChange}
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
                    actions.cancel?.(providedPlatform.platformId!);
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
