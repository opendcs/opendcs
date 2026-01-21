import { Card } from "react-bootstrap";
import type { ApiSite } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";


interface SiteProperties {
    site?: ApiSite
}

export const Site: React.FC<SiteProperties> = ({site}) => {

    return (
        <Card>
            <Card.Title>Site - ({site ? "would get default name" : "new"})</Card.Title>
            <Card.Body>
            {JSON.stringify(site)}
            </Card.Body>
        </Card>
    );
};

export default Site;