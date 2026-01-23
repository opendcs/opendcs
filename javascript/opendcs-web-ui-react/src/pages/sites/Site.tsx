import { Card, Col, Form, FormControl, FormGroup, FormSelect, Row } from "react-bootstrap";
import { PropertiesTable, type Property } from "../../components/properties";
import { useState } from "react";
import type { ApiSite } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useTranslation } from "react-i18next";
import { SiteNameList } from "./SiteNameList";
import type { Actions } from "../../util/Actions";


interface SiteProperties {
    site?: ApiSite
}

const elevationUnits = [
    {units: "m", name: "M (Meters)"},
    {units: "cm", name: "cM (Centimeters)"},
    {units: "ft", name: "ft (Feet)"},
    {units: "in", name: "in (Inches)"},
    {units: "km", name: "kM (Kilometers)"},
    {units: "mm", name: "mM (Millimeters)"},
    {units: "mi", name: "mi (Miles)"},
    {units: "nmi", name: "nmi (Nautical Miles)"},
    {units: "um", name: "uM (Micrometers)"},
    {units: "yd", name: "yd (Yards)"}
];

export const Site: React.FC<SiteProperties> = ({site}) => {
    const {t} = useTranslation();
    const [realSite, _updateSite] = useState(site ? site : {properties: {}});
    const [props, _updateProps] = useState(realSite.properties ?
                                    Object.values(realSite.properties).map(([k, v]) => {
                                        return {name: k, value: v}
                                    })
                                    : []);
    const editMode = site ? false : true;

    const propertyActions: Actions<Property,string> = editMode ? {
        add: () => {},
        edit: () => {},
        remove: () => {},
        save: () => {}
    } : {};

    const siteNameActions: Actions<String> = editMode ? {
        add: () => {},
        edit: () => {},
        remove: () => {},
        save: () => {}
    }: {};

    return (
        <Card>
            <Card.Body>
                <Row>
                    <Col>
                        <Row><SiteNameList siteNames={realSite.sitenames || {}} actions={siteNameActions}/></Row>
                        <Row>{/* TODO: need to have list be read only unless we're in edit mode */}
                            <PropertiesTable theProps={props} actions={propertyActions} width={"100%"} />
                        </Row>
                    </Col>
                    <Col>

                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="latitude">{t("latitude")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="latitude" name="latitude" readOnly={!editMode}
                                      placeholder={t("sites:use_decimal_format")}
                                      defaultValue={realSite.latitude}/>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="longitude">{t("longitude")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="longitude" name="longitude" readOnly={!editMode}
                                      placeholder={t("sites:use_decimal_format")}
                                      defaultValue={realSite.longitude}/>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="elevation">{t("elevation")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="elevation" name="elevation " readOnly={!editMode}
                                      defaultValue={realSite.elevation} />
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="elevationUnits">{t("elevation_units")}</Form.Label>
                        <Col sm={10}>
                        <FormSelect id="elevationUnits" disabled={!editMode}>
                            {elevationUnits.map((unit) => {
                                return (<option key={unit.units} value={unit.units}
                                                selected={realSite.elevUnits === unit.units}>
                                            {unit.name}
                                        </option>
                                );
                            })}
                        </FormSelect>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="nearestCity">{t("sites:nearestCity")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="nearestCity" name="nearest_city" readOnly={!editMode}
                                      defaultValue={realSite.nearestCity}/>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="state">{t("state")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="state" name="state" readOnly={!editMode}
                                      defaultValue={realSite.state}/>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="country">{t("country")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="country" name="country" readOnly={!editMode}
                                      placeholder={t("sites:enter_country")}
                                      defaultValue={realSite.country}/>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="region">{t("Region")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="region" name="region" readOnly={!editMode}
                                      placeholder={t("sites:enter_region")}
                                      defaultValue={realSite.region}/>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="publicName">{t("sites:public_name")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="publicName" name="publicName" readOnly={!editMode}
                                      defaultValue={realSite.publicName}/>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="description">{t("description")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="description" name="description" readOnly={!editMode}
                                      defaultValue={realSite.description}/>
                        </Col>
                    </FormGroup>



                    </Col>
                </Row>
                <Row>
                    <Col>Controls?</Col>
                </Row>

                {JSON.stringify(site)}
            </Card.Body>
        </Card>
    );
};

export default Site;