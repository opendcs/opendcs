import Alert from "react-bootstrap/Alert";
import Card from "react-bootstrap/Card";
import Col from "react-bootstrap/Col";
import Form from "react-bootstrap/Form";
import Row from "react-bootstrap/Row";
import Spinner from "react-bootstrap/Spinner";
import Stack from "react-bootstrap/Stack";
import { DEFAULT_GROUP } from "../constants";
import { useStatusGroupSummary } from "../hooks/useStatusGroupSummary";
import { DcpLocationAccordion } from "./DcpLocationAccordion";
import { SummaryCards } from "./SummaryCards";

export function DcpMonDashboard() {
  const summary = useStatusGroupSummary(DEFAULT_GROUP);

  if (summary.isLoading) {
    return (
      <div className="d-flex align-items-center gap-2 p-4 text-secondary">
        <Spinner animation="border" size="sm" />
        Loading DCP status
      </div>
    );
  }

  if (summary.isError || !summary.data) {
    return <Alert variant="danger">Unable to load DCPMon status summary.</Alert>;
  }

  const dcpSummaries = Object.entries(summary.data.dcpSummaries ?? {});
  const lowBatteryAddresses = dcpSummaries
    .filter(([, dcpSummary]) => dcpSummary.lowBattery)
    .map(([dcpAddress]) => dcpAddress);
  const durationHours = summary.data.durationHours ?? 0;

  return (
    <Stack gap={4}>
      <Row className="align-items-end g-3">
        <Col>
          <h1 className="h3 mb-1">DCPMon</h1>
          <div className="text-secondary">
            Group {DEFAULT_GROUP.toUpperCase()} for the last {durationHours} hours
          </div>
        </Col>
        <Col md={3}>
          <Form.Label htmlFor="dcpmon-group" className="small text-secondary">
            Group
          </Form.Label>
          <Form.Select id="dcpmon-group" value={DEFAULT_GROUP.toUpperCase()} disabled>
            <option>{DEFAULT_GROUP.toUpperCase()}</option>
          </Form.Select>
        </Col>
      </Row>

      <SummaryCards summary={summary.data} />

      {lowBatteryAddresses.length > 0 && (
        <Alert variant="warning" className="mb-0">
          Low battery: {lowBatteryAddresses.join(", ")}
        </Alert>
      )}

      <Card>
        <Card.Header>
          <Card.Title as="h2" className="h5 mb-0">
            Stations
          </Card.Title>
        </Card.Header>
        <Card.Body>
          {dcpSummaries.map(([dcpAddress, dcpSummary]) => (
            <DcpLocationAccordion
              key={dcpAddress}
              dcpAddress={dcpAddress}
              summary={dcpSummary}
              totalHours={durationHours}
            />
          ))}
        </Card.Body>
      </Card>
    </Stack>
  );
}
