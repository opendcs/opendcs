import { useState } from "react";
import Alert from "react-bootstrap/Alert";
import Card from "react-bootstrap/Card";
import Col from "react-bootstrap/Col";
import Form from "react-bootstrap/Form";
import Row from "react-bootstrap/Row";
import Spinner from "react-bootstrap/Spinner";
import Stack from "react-bootstrap/Stack";
import { PREFERRED_GROUP } from "../constants";
import { useDataGroups } from "../hooks/useDataGroups";
import { useStatusGroupSummary } from "../hooks/useStatusGroupSummary";
import { DcpLocationAccordion } from "./DcpLocationAccordion";
import { SummaryCards } from "./SummaryCards";

export function DcpMonDashboard() {
  const groups = useDataGroups();
  const [requestedGroup, setRequestedGroup] = useState("");
  const preferredGroup = groups.data?.find(
    (group) => group.id.toLowerCase() === PREFERRED_GROUP.toLowerCase(),
  );
  const selectedGroup = groups.data?.some((group) => group.id === requestedGroup)
    ? requestedGroup
    : (preferredGroup ?? groups.data?.[0])?.id ?? "";
  const summary = useStatusGroupSummary(selectedGroup);

  if (groups.isLoading || (selectedGroup && summary.isLoading)) {
    return (
      <div className="d-flex align-items-center gap-2 p-4 text-secondary">
        <Spinner animation="border" size="sm" />
        Loading DCP status
      </div>
    );
  }

  if (groups.isError) {
    return <Alert variant="danger">Unable to load DCPMon groups.</Alert>;
  }

  if (!groups.data?.length) {
    return <Alert variant="warning">No DCP status groups are configured.</Alert>;
  }

  if (!selectedGroup) {
    return null;
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
            Group {selectedGroup} for the last {durationHours} hours
          </div>
        </Col>
        <Col md={3}>
          <Form.Label htmlFor="dcpmon-group" className="small text-secondary">
            Group
          </Form.Label>
          <Form.Select
            id="dcpmon-group"
            value={selectedGroup}
            onChange={(event) => setRequestedGroup(event.target.value)}
            disabled={groups.data.length < 2}
          >
            {groups.data.map((group) => (
              <option key={group.id} value={group.id}>
                {group.displayName}
              </option>
            ))}
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
            />
          ))}
        </Card.Body>
      </Card>
    </Stack>
  );
}
