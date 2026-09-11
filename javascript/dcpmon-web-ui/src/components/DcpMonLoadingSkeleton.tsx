import Accordion from "react-bootstrap/Accordion";
import Badge from "react-bootstrap/Badge";
import Button from "react-bootstrap/Button";
import Card from "react-bootstrap/Card";
import Col from "react-bootstrap/Col";
import Form from "react-bootstrap/Form";
import InputGroup from "react-bootstrap/InputGroup";
import Row from "react-bootstrap/Row";
import Stack from "react-bootstrap/Stack";
import { Gear, Search } from "react-bootstrap-icons";
import type { DataGroup } from "opendcs-dds-api";

const summaryCards = [
  ["complete", "Complete", "success"],
  ["partial", "Partial", "warning"],
  ["parity", "Parity", "info"],
  ["missing", "Missing", "danger"],
] as const;

const categories = [
  ["missing", "Missing data", "danger"],
  ["parity", "Parity data", "info"],
  ["partial", "Partial data", "warning"],
  ["complete", "Complete data", "success"],
] as const;

type DcpMonLoadingSkeletonProps = {
  groups?: DataGroup[];
  selectedGroup: string;
  onGroupChange: (group: string) => void;
  onSettingsClick: () => void;
};

function LoadingLine({ width }: { width: string }) {
  return <span className="dcpmon-skeleton-line" style={{ width }} />;
}

export function DcpMonLoadingSkeleton({
  groups,
  selectedGroup,
  onGroupChange,
  onSettingsClick,
}: DcpMonLoadingSkeletonProps) {
  return (
    <Stack gap={4} aria-busy="true">
      <span className="visually-hidden" role="status">
        Loading DCP status
      </span>

      <Row className="dcpmon-dashboard-header align-items-end g-3">
        <Col xs={12} md>
          <h1 className="h3 mb-1">DCPMon</h1>
          <div className="d-flex flex-column gap-2 py-1" aria-hidden="true">
            <LoadingLine width="17rem" />
            <LoadingLine width="11rem" />
          </div>
        </Col>
        <Col xs="auto" md="auto">
          <Button
            variant="outline-secondary"
            className="dcpmon-settings-button d-flex align-items-center gap-2"
            onClick={onSettingsClick}
            aria-label="Settings"
            title="Display settings"
          >
            <Gear aria-hidden="true" />
            <span className="dcpmon-settings-label">Settings</span>
          </Button>
        </Col>
        <Col xs md={3}>
          <Form.Label htmlFor="dcpmon-group" className="small text-secondary">
            Group
          </Form.Label>
          <Form.Select
            id="dcpmon-group"
            value={selectedGroup}
            onChange={(event) => onGroupChange(event.target.value)}
            disabled={!groups || groups.length < 2}
          >
            {!groups?.length && <option value="">Loading groups…</option>}
            {groups?.map((group) => (
              <option key={group.id} value={group.id}>
                {group.displayName}
              </option>
            ))}
          </Form.Select>
        </Col>
      </Row>

      <div className="dcpmon-summary-grid dcpmon-summary-grid--4 mb-4">
        {summaryCards.map(([, label, variant]) => (
          <Card
            key={label}
            className={`dcpmon-summary-card text-bg-${variant}`}
          >
            <Card.Body>
              <div className="dcpmon-summary-label text-uppercase">{label}</div>
              <span className="dcpmon-skeleton-value" aria-hidden="true" />
            </Card.Body>
          </Card>
        ))}
      </div>

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
              placeholder="Loading locations…"
              disabled
            />
          </InputGroup>
        </Card.Body>
      </Card>

      <Accordion
        alwaysOpen
        defaultActiveKey={categories.map(([eventKey]) => eventKey)}
        className="dcpmon-status-sections"
      >
        {categories.map(([eventKey, label, variant]) => (
          <Accordion.Item key={eventKey} eventKey={eventKey}>
            <Accordion.Header>
              <span className="d-flex align-items-center justify-content-between w-100 pe-3">
                <span className="fw-semibold">{label}</span>
                <Badge bg={variant}>— locations</Badge>
              </span>
            </Accordion.Header>
            <Accordion.Body>
              <div className="dcpmon-station-skeleton" aria-hidden="true">
                <LoadingLine width="5rem" />
                <LoadingLine width="8rem" />
                <LoadingLine width="35%" />
                <LoadingLine width="3.5rem" />
              </div>
            </Accordion.Body>
          </Accordion.Item>
        ))}
      </Accordion>
    </Stack>
  );
}
