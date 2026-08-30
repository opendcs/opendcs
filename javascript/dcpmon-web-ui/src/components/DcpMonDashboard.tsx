import { useState } from "react";
import Accordion from "react-bootstrap/Accordion";
import Alert from "react-bootstrap/Alert";
import Badge from "react-bootstrap/Badge";
import Button from "react-bootstrap/Button";
import Card from "react-bootstrap/Card";
import Col from "react-bootstrap/Col";
import Form from "react-bootstrap/Form";
import InputGroup from "react-bootstrap/InputGroup";
import Row from "react-bootstrap/Row";
import Spinner from "react-bootstrap/Spinner";
import Stack from "react-bootstrap/Stack";
import { Search, X } from "react-bootstrap-icons";
import type { DcpSummary } from "opendcs-dds-api";
import { PREFERRED_GROUP } from "../constants";
import { useDataGroups } from "../hooks/useDataGroups";
import { useStatusGroupSummary } from "../hooks/useStatusGroupSummary";
import { DcpLocationAccordion } from "./DcpLocationAccordion";
import { SummaryCards } from "./SummaryCards";

type StationEntry = [string, DcpSummary];
type DcpStatus = DcpSummary["status"];

const statusOrder: DcpStatus[] = [
  "missing",
  "parity",
  "partial",
  "unknown",
  "complete",
];

const statusLabels: Record<DcpStatus, string> = {
  missing: "Missing data",
  parity: "Parity data",
  partial: "Partial data",
  unknown: "Unknown schedule",
  complete: "Complete data",
};

const statusVariants: Record<DcpStatus, string> = {
  missing: "danger",
  parity: "info",
  partial: "warning",
  unknown: "secondary",
  complete: "success",
};

function StationList({ stations }: { stations: StationEntry[] }) {
  return stations.map(([dcpAddress, stationSummary]) => (
    <DcpLocationAccordion
      key={dcpAddress}
      dcpAddress={dcpAddress}
      summary={stationSummary}
    />
  ));
}

function matchesSearch([dcpAddress, stationSummary]: StationEntry, query: string) {
  if (!query) return true;
  return [
    dcpAddress,
    ...(stationSummary.identifiers?.map((identifier) => identifier.id) ?? []),
  ].some((value) => value.toLocaleLowerCase().includes(query));
}

export function DcpMonDashboard() {
  const groups = useDataGroups();
  const [requestedGroup, setRequestedGroup] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
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

  if (!selectedGroup) return null;

  if (summary.isError || !summary.data) {
    return <Alert variant="danger">Unable to load DCPMon status summary.</Alert>;
  }

  const stations = Object.entries(summary.data.dcpSummaries ?? {}) as StationEntry[];
  const normalizedSearch = searchQuery.trim().toLocaleLowerCase();
  const filteredStations = stations.filter((station) =>
    matchesSearch(station, normalizedSearch),
  );
  const lowBatteryAddresses = stations
    .filter(([, stationSummary]) => stationSummary.lowBattery)
    .map(([dcpAddress]) => dcpAddress);
  const durationHours = summary.data.durationHours ?? 0;
  const updated = summary.data.timestamp.toLocaleString();

  return (
    <Stack gap={4}>
      <Row className="align-items-end g-3">
        <Col>
          <h1 className="h3 mb-1">DCPMon</h1>
          <div className="text-secondary">
            Group {selectedGroup} for the last {durationHours} hours · {stations.length}{" "}
            locations
          </div>
          <div className="small text-secondary">Updated {updated}</div>
        </Col>
        <Col md={3}>
          <Form.Label htmlFor="dcpmon-group" className="small text-secondary">
            Group
          </Form.Label>
          <Form.Select
            id="dcpmon-group"
            value={selectedGroup}
            onChange={(event) => {
              setRequestedGroup(event.target.value);
              setSearchQuery("");
            }}
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
        <Card.Body>
          <Form.Label htmlFor="dcpmon-search" className="fw-semibold">
            Search locations
          </Form.Label>
          <InputGroup>
            <InputGroup.Text aria-hidden="true">
              <Search />
            </InputGroup.Text>
            <Form.Control
              id="dcpmon-search"
              type="search"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Local ID, configured description, or NESDIS address"
              aria-describedby="dcpmon-search-help"
            />
            {searchQuery && (
              <Button
                variant="outline-secondary"
                onClick={() => setSearchQuery("")}
                aria-label="Clear search"
              >
                <X aria-hidden="true" />
              </Button>
            )}
          </InputGroup>
          <Form.Text id="dcpmon-search-help">
            Search is not case-sensitive and matches every configured identifier.
          </Form.Text>
        </Card.Body>
      </Card>

      {normalizedSearch ? (
        <Card>
          <Card.Header className="d-flex align-items-center justify-content-between">
            <Card.Title as="h2" className="h5 mb-0">
              Search results
            </Card.Title>
            <Badge bg="primary">{filteredStations.length} locations</Badge>
          </Card.Header>
          <Card.Body>
            {filteredStations.length ? (
              <StationList stations={filteredStations} />
            ) : (
              <Alert variant="secondary" className="mb-0">
                No locations match “{searchQuery.trim()}”.
              </Alert>
            )}
          </Card.Body>
        </Card>
      ) : (
        <Accordion
          alwaysOpen
          defaultActiveKey={["missing", "parity", "partial", "unknown"]}
          className="dcpmon-status-sections"
        >
          {statusOrder.map((status) => {
            const statusStations = stations.filter(
              ([, stationSummary]) => stationSummary.status === status,
            );
            return (
              <Accordion.Item key={status} eventKey={status}>
                <Accordion.Header>
                  <span className="d-flex align-items-center justify-content-between w-100 pe-3">
                    <span className="fw-semibold">{statusLabels[status]}</span>
                    <Badge bg={statusVariants[status]}>
                      {statusStations.length} locations
                    </Badge>
                  </span>
                </Accordion.Header>
                <Accordion.Body>
                  {statusStations.length ? (
                    <StationList stations={statusStations} />
                  ) : (
                    <span className="text-secondary">No locations</span>
                  )}
                </Accordion.Body>
              </Accordion.Item>
            );
          })}
        </Accordion>
      )}
    </Stack>
  );
}
