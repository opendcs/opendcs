import { Button, Card, Col, Form, FormGroup, FormSelect, Row } from "react-bootstrap";
import { PropertiesTable, type Property } from "../../components/properties";
import { use, useMemo, useReducer, useState } from "react";
import type { ApiSite } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useTranslation } from "react-i18next";
import { SiteNameList, type SiteNameType } from "./SiteNameList";
import type { CollectionActions, SaveAction, UiState } from "../../util/Actions";
import { SiteReducer } from "./SiteReducer";

export type UiSite = Partial<ApiSite & { ui_state?: UiState }>;

interface SiteProperties {
  site: Promise<UiSite> | UiSite;
  actions?: SaveAction<ApiSite>;
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

export const Site: React.FC<SiteProperties> = ({ site, actions = {} }) => {
  const { t } = useTranslation();
  const providedSite = site instanceof Promise ? use(site) : site;
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [localSite, dispatch] = useReducer(SiteReducer, providedSite);

  const editMode = providedSite.ui_state ? false : true;

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [props, updateProps] = useState<Property[]>(() => {
    const props = localSite.properties || {};
    return Object.values(props).map(([k, v]) => {
      return { name: k as string, value: v as string };
    });
  });

  const propertyActions: CollectionActions<Property, string> = editMode
    ? {
        add: () => {
          updateProps((prev) => {
            const existingNew = prev
              .filter((p: Property) => p.state === "new")
              .sort((a: Property, b: Property) => {
                return parseInt(a.name) - parseInt(b.name);
              });
            const idx = existingNew.length > 0 ? parseInt(existingNew[0].name) + 1 : 1;

            const tmp = [
              ...prev,
              { name: `${idx}`, value: "", state: "new" } as Property,
            ];
            return tmp;
          });
        },
        edit: (propName) => {
          updateProps((prev) => {
            const tmp: Property[] = prev.map((p: Property) => {
              if (p.name === propName) {
                return {
                  ...p,
                  state: "edit",
                };
              } else {
                return p;
              }
            });
            return tmp;
          });
        },
        remove: () => {},
        save: (prop) => {
          dispatch({
            type: "save_prop",
            payload: { name: prop.name, value: prop.value as string },
          });
        },
      }
    : {};

  const siteNameActions: CollectionActions<SiteNameType> = editMode
    ? {
        add: (siteName?: SiteNameType) => {
          dispatch({ type: "add_name", payload: siteName! });
        },
        //edit: () => {},
        //remove: () => {},
        save: (siteName: SiteNameType) => {
          dispatch({ type: "add_name", payload: siteName! });
        },
      }
    : {};

  const siteNames = useMemo<SiteNameType[]>(() => {
    return Object.entries(localSite.sitenames || {})
                 .map(([k,v]) => {
                    return {type: k, name: v}
                  });
  }, [localSite.sitenames]);

  return (
    <Card>
      <Card.Body>
        <Row>
          <Col>
            <Row>
              <SiteNameList
                siteNames={siteNames}
                actions={siteNameActions}
              />
            </Row>
            <Row>
              {/* TODO: need to have list be read only unless we're in edit mode */}
              <PropertiesTable
                theProps={props}
                actions={propertyActions}
                width={"100%"}
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
                  readOnly={!editMode}
                  placeholder={t("sites:use_decimal_format")}
                  defaultValue={localSite.latitude}
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
                  readOnly={!editMode}
                  placeholder={t("sites:use_decimal_format")}
                  defaultValue={localSite.longitude}
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
                  name="elevation "
                  readOnly={!editMode}
                  defaultValue={localSite.elevation}
                />
              </Col>
            </FormGroup>
            <FormGroup as={Row} className="mb-3">
              <Form.Label column sm={2} htmlFor="elevationUnits">
                {t("elevation_units")}
              </Form.Label>
              <Col sm={10}>
                <FormSelect id="elevationUnits" disabled={!editMode}>
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
                  name="nearest_city"
                  readOnly={!editMode}
                  defaultValue={localSite.nearestCity}
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
                  readOnly={!editMode}
                  defaultValue={localSite.state}
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
                  readOnly={!editMode}
                  placeholder={t("sites:enter_country")}
                  defaultValue={localSite.country}
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
                  readOnly={!editMode}
                  placeholder={t("sites:enter_region")}
                  defaultValue={localSite.region}
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
                  readOnly={!editMode}
                  defaultValue={localSite.publicName}
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
                  readOnly={!editMode}
                  defaultValue={localSite.description}
                />
              </Col>
            </FormGroup>
          </Col>
        </Row>
        <Row>
          <Col>
            <Button onClick={() => actions.save?.(localSite)} variant="primary">
              Save
            </Button>
          </Col>
        </Row>
      </Card.Body>
    </Card>
  );
};

export default Site;
