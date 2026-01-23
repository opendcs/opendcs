import { Card, Col, Form, FormControl, FormGroup, FormSelect, Row } from "react-bootstrap";
import { PropertiesTable, type Property } from "../../components/properties";
import { useState } from "react";
import type { ApiSite } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useTranslation } from "react-i18next";
import { SiteNameList } from "./SiteNameList";


interface SiteProperties {
    site?: ApiSite
}

export const Site: React.FC<SiteProperties> = ({site}) => {
    const {t} = useTranslation();
    const [realSite, _updateSite] = useState(site ? site : {properties: {}});
    const [props, _updateProps] = useState(realSite.properties ?
                                    Object.values(realSite.properties).map(([k, v]) => {
                                        return {name: k, value: v}
                                    })
                                    : []);
    const editMode = site ? false : true;

    return (
        <Card>
            <Card.Body>
                <Row>
                    <Col>
                        <Row><SiteNameList siteNames={realSite.sitenames || {}}/></Row>
                        <Row>{/* TODO: need to have list be read only unless we're in edit mode */}
                            <PropertiesTable theProps={props} saveProp={function (prop: Property): void {
                                throw new Error("Function not implemented.");
                            } } removeProp={function (prop: string): void {
                                throw new Error("Function not implemented.");
                            } } addProp={function (): void {
                                throw new Error("Function not implemented.");
                            } } editProp={function (prop: string): void {
                                throw new Error("Function not implemented.");
                            } } width={"100%"} />
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
                        <Form.Control type="text" id="elevation" name="elevation " readOnly={!editMode} />
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="elevationUnits">{t("elevation_units")}</Form.Label>
                        <Col sm={10}>
                        <FormSelect id="elevationUnits" disabled={!editMode}>
                            <option value="m">M (Meters)</option>
                            <option value="cm">cM (Centimeters)</option>
                            <option value="ft">ft (Feet)</option>
                            <option value="in">in (Inches)</option>
                            <option value="km">kM (Kilometers)</option>
                            <option value="mm">mM (Millimeters)</option>
                            <option value="mi">mi (Miles)</option>
                            <option value="nmi">nmi (Nautical Miles)</option>
                            <option value="um">uM (Micrometers)</option>
                            <option value="yd">yd (Yards)</option>
                        </FormSelect>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="nearestCity">{t("sites:nearestCity")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="nearestCity" name="nearest_city" readOnly={!editMode}/>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="state">{t("state")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="state" name="state" readOnly={!editMode}/>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="country">{t("country")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="country" name="country" readOnly={!editMode}
                                      placeholder={t("sites:enter_country")}/>
                        </Col>
                    </FormGroup>
                    <FormGroup as={Row} className="mb-3">
                        <Form.Label column sm={2} htmlFor="region">{t("Region")}</Form.Label>
                        <Col sm={10}>
                        <Form.Control type="text" id="region" name="region" readOnly={!editMode}
                                      placeholder={t("sites:enter_region")}/>
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