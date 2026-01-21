import { Card, Row } from "react-bootstrap";
import { PropertiesTable, type Property } from "../../components/properties";
import { useState } from "react";
import type { ApiSite } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";


interface SiteProperties {
    site?: ApiSite
}

export const Site: React.FC<SiteProperties> = ({site}) => {
    const [realSite, _updateSite] = useState(site ? site : {properties: {}});

    const propsList: Property[] = Object.values(realSite.properties!).map(([k, v]) => {
        return {name: k, value: v}
    });

    return (
        <Card>
            <Card.Title>Site - ({site ? "would get default name" : "new"})</Card.Title>
            <Card.Body>
                <Row>
                    
                </Row>
                <Row>
                    <PropertiesTable theProps={propsList} saveProp={function (prop: Property): void {
                        throw new Error("Function not implemented.");
                    } } removeProp={function (prop: string): void {
                        throw new Error("Function not implemented.");
                    } } addProp={function (): void {
                        throw new Error("Function not implemented.");
                    } } editProp={function (prop: string): void {
                        throw new Error("Function not implemented.");
                    } } />
                </Row>

            {JSON.stringify(site)}
            </Card.Body>
        </Card>
    );
};

export default Site;