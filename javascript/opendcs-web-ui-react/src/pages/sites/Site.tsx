import {
  Button,
  Card,
  Col,
  Form,
  FormGroup,
  FormSelect,
  Placeholder,
  Row,
} from "react-bootstrap";
import { PropertiesTable, type Property } from "../../components/properties";
import { use, useCallback, useEffect, useMemo, useReducer } from "react";
import type { ApiSite } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useTranslation } from "react-i18next";
import { SiteNameList, type SiteNameType } from "./SiteNameList";
import type { CancelAction, CollectionActions, SaveAction } from "../../util/Actions";
import { SiteReducer } from "./SiteReducer";
import { Save, X } from "react-bootstrap-icons";
import { DetailFade } from "../../components/data-table";

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };

const SkeletonMiniTable: React.FC<{
  rows: number;
  withHeader?: boolean;
  withCaption?: boolean;
  withSearch?: boolean;
}> = ({ rows, withHeader = true, withCaption = true, withSearch = false }) => (
  <>
    {withSearch && (
      <div className="d-flex justify-content-end align-items-center mb-2">
        <Placeholder animation="glow" style={{ width: "20%" }}>
          <Placeholder xs={12} className="rounded" style={{ height: "1.875rem" }} />
        </Placeholder>
      </div>
    )}
    {withCaption && (
      <Placeholder
        as="div"
        animation="glow"
        className="mb-1 d-flex justify-content-center"
      >
        <Placeholder xs={4} className="rounded" style={LABEL_H} />
      </Placeholder>
    )}
    <div className="border rounded overflow-hidden">
      {withHeader && (
        <div
          className="d-flex align-items-center px-2"
          style={{ height: "2rem", background: "var(--odcs-table-header-bg)" }}
        >
          {[0, 1].map((c) => (
            <Placeholder key={c} animation="glow" className="flex-fill me-2">
              <Placeholder xs={6} className="rounded" style={{ height: "0.75rem" }} />
            </Placeholder>
          ))}
        </div>
      )}
      <Placeholder animation="glow" className="d-block">
        <Placeholder xs={12} style={{ height: `calc(${rows} * 2rem)` }} />
      </Placeholder>
    </div>
  </>
);

const SITE_FIELDS = [
  "latitude",
  "longitude",
  "elevation",
  "elevationUnits",
  "nearestCity",
  "timezone",
  "state",
  "country",
  "region",
  "publicName",
  "description",
] as const;

const TIMEZONES: readonly string[] = [
  "UTC",
  "GMT",
  "US/Eastern",
  "US/Central",
  "US/Mountain",
  "US/Pacific",
  "US/Alaska",
  "US/Hawaii",
  "US/Arizona",
  "America/New_York",
  "America/Chicago",
  "America/Denver",
  "America/Los_Angeles",
  "America/Phoenix",
  "America/Anchorage",
  "America/Honolulu",
  "America/Puerto_Rico",
  "Europe/London",
];
const SITE_TIMEZONE_LIST_ID = "site-timezone-list";

export const SiteSkeleton: React.FC<{ edit?: boolean; className?: string }> = ({
  edit = false,
  className,
}) => (
  <Card
    className={["site-card", edit ? "site-card--edit" : null, className]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        {/* Left column — SiteNameList + PropertiesTable */}
        <Col>
          <Row className="mb-3">
            <Col>
              <SkeletonMiniTable rows={3} withCaption={false} />
            </Col>
          </Row>
          <Row>
            <Col>
              <SkeletonMiniTable rows={5} withSearch />
            </Col>
          </Row>
        </Col>

        {/* Right column — 10 label/input rows */}
        <Col>
          {SITE_FIELDS.map((field) => (
            <Row key={field} className="mb-3 align-items-center">
              <Placeholder as={Col} sm={2} animation="glow">
                <Placeholder xs={10} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} sm={10} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
          ))}
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

export type UiSite = Partial<ApiSite>;

export interface SiteProperties {
  site: Promise<UiSite> | UiSite;
  actions?: SaveAction<ApiSite> & CancelAction<number>;
  edit?: boolean;
}

const elevationUnits = [
  { units: "m", name: "M (Meters)" },
  { units: "cm", name: "cM (Centimeters)" },
  { units: "ft", name: "ft (Feet)" },
  { units: "in", name: "in (Inches)" },
  { units: "km", name: "kM (Kilometers)" },
  { units: "mm", name: "mM (Millimeters)" },
  { units: "mi", name: "mi (Miles)" },
  { units: "nmi", name: "nmi (Nautical Miles)" },
  { units: "um", name: "uM (Micrometers)" },
  { units: "yd", name: "yd (Yards)" },
];

const mapProps: (props: { [k: string]: string }) => Property[] = (props) => {
  return Object.entries(props).map(([k, v]): Property => {
    return { name: k, value: v };
  });
};

export const Site: React.FC<SiteProperties> = ({
  site,
  actions = {},
  edit = false,
}) => {
  const { t } = useTranslation();
  const providedSite = site instanceof Promise ? use(site) : site;
  const [localSite, dispatch] = useReducer(SiteReducer, providedSite);

  const props = useMemo(
    () => mapProps(localSite.properties || {}),
    [localSite.properties],
  );

  // need an initial set of the units if it isn't defined as it's a required field in the database.
  useEffect(() => {
    if (providedSite.elevUnits === undefined) {
      dispatch({ type: "save", payload: { elevUnits: elevationUnits[0].units } });
    }
  }, []);

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

  const siteNameActions: CollectionActions<SiteNameType> = edit
    ? {
        add: (siteName?: SiteNameType) => {
          dispatch({ type: "add_name", payload: siteName! });
        },
        //edit: () => {},
        remove: (snt: SiteNameType) => {
          dispatch({ type: "delete_name", payload: { type: snt.type } });
        },
        save: (siteName: SiteNameType) => {
          dispatch({ type: "add_name", payload: siteName! });
        },
      }
    : {};

  const siteNames = useMemo<SiteNameType[]>(() => {
    return Object.entries(localSite.sitenames || {}).map(([k, v]) => {
      return { type: k, name: v };
    });
  }, [localSite.sitenames]);

  const saveSite = useCallback((site: UiSite) => {
    actions.save!(site as ApiSite);
  }, []);

  const inputChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const { name, value } = event.target;
      dispatch({ type: "save", payload: { [name]: value } });
    },
    [dispatch],
  );

  const selectChange = useCallback(
    (event: React.ChangeEvent<HTMLSelectElement>) => {
      const { name, value } = event.target;
      dispatch({ type: "save", payload: { [name]: value } });
    },
    [dispatch],
  );

  return (
    <DetailFade skeleton={<SiteSkeleton edit={edit} />}>
      <datalist id={SITE_TIMEZONE_LIST_ID}>
        {TIMEZONES.map((tz) => (
          <option key={tz} value={tz} />
        ))}
      </datalist>
      <Card
        className={["site-card", edit ? "site-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Row>
            <Col>
              <Row>
                <SiteNameList
                  siteNames={siteNames}
                  actions={siteNameActions}
                  edit={edit}
                />
              </Row>
              <Row>
                <PropertiesTable
                  theProps={props}
                  actions={propertyActions}
                  edit={edit}
                  canAdd={true}
                  width={"100%"}
                  height={"auto"}
                />
              </Row>
            </Col>
            <Col>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={2} htmlFor="latitude">
                  {t("latitude")}
                </Form.Label>
                <Col sm={10}>
                  <Form.Control
                    type="text"
                    id="latitude"
                    name="latitude"
                    readOnly={!edit}
                    placeholder={t("sites:use_decimal_format")}
                    defaultValue={localSite.latitude}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={2} htmlFor="longitude">
                  {t("longitude")}
                </Form.Label>
                <Col sm={10}>
                  <Form.Control
                    type="text"
                    id="longitude"
                    name="longitude"
                    readOnly={!edit}
                    placeholder={t("sites:use_decimal_format")}
                    defaultValue={localSite.longitude}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={2} htmlFor="elevation">
                  {t("elevation")}
                </Form.Label>
                <Col sm={10}>
                  <Form.Control
                    type="text"
                    id="elevation"
                    name="elevation"
                    readOnly={!edit}
                    defaultValue={localSite.elevation}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={2} htmlFor="elevationUnits">
                  {t("elevation_units")}
                </Form.Label>
                <Col sm={10}>
                  <FormSelect
                    id="elevationUnits"
                    disabled={!edit}
                    name="elevUnits"
                    onChange={selectChange}
                  >
                    {elevationUnits.map((unit) => {
                      return (
                        <option
                          key={unit.units}
                          value={unit.units}
                          selected={localSite.elevUnits === unit.units}
                        >
                          {unit.name}
                        </option>
                      );
                    })}
                  </FormSelect>
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={2} htmlFor="nearestCity">
                  {t("sites:nearestCity")}
                </Form.Label>
                <Col sm={10}>
                  <Form.Control
                    type="text"
                    id="nearestCity"
                    name="nearestCity"
                    readOnly={!edit}
                    defaultValue={localSite.nearestCity}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={2} htmlFor="timezone">
                  {t("timezone")}
                </Form.Label>
                <Col sm={10}>
                  <Form.Control
                    type="text"
                    id="timezone"
                    name="timezone"
                    readOnly={!edit}
                    list={SITE_TIMEZONE_LIST_ID}
                    defaultValue={localSite.timezone ?? ""}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={2} htmlFor="state">
                  {t("state")}
                </Form.Label>
                <Col sm={10}>
                  <Form.Control
                    type="text"
                    id="state"
                    name="state"
                    readOnly={!edit}
                    defaultValue={localSite.state}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={2} htmlFor="country">
                  {t("country")}
                </Form.Label>
                <Col sm={10}>
                  <Form.Control
                    type="text"
                    id="country"
                    name="country"
                    readOnly={!edit}
                    placeholder={t("sites:enter_country")}
                    defaultValue={localSite.country}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={2} htmlFor="region">
                  {t("region")}
                </Form.Label>
                <Col sm={10}>
                  <Form.Control
                    type="text"
                    id="region"
                    name="region"
                    readOnly={!edit}
                    placeholder={t("sites:enter_region")}
                    defaultValue={localSite.region}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={2} htmlFor="publicName">
                  {t("sites:public_name")}
                </Form.Label>
                <Col sm={10}>
                  <Form.Control
                    type="text"
                    id="publicName"
                    name="publicName"
                    readOnly={!edit}
                    defaultValue={localSite.publicName}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={2} htmlFor="description">
                  {t("description")}
                </Form.Label>
                <Col sm={10}>
                  <Form.Control
                    type="text"
                    id="description"
                    name="description"
                    readOnly={!edit}
                    defaultValue={localSite.description}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>
            </Col>
          </Row>
          {edit && (
            <Row>
              <Col className="d-flex justify-content-end">
                <Button
                  onClick={() => {
                    actions.cancel?.(providedSite.siteId!);
                  }}
                  variant="secondary"
                  aria-label={t("sites:cancel_for", { id: providedSite.siteId })}
                >
                  <X /> {t("cancel")}
                </Button>
                <Button
                  onClick={() => saveSite(localSite)}
                  variant="primary"
                  aria-label={t("sites:save_site", { id: localSite.siteId })}
                >
                  <Save /> {t("save")}
                </Button>
              </Col>
            </Row>
          )}
        </Card.Body>
      </Card>
    </DetailFade>
  );
};

export default Site;
