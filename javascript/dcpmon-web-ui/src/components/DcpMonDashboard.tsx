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
import Stack from "react-bootstrap/Stack";
import { Gear, Search, X } from "react-bootstrap-icons";
import type { DcpSummary } from "opendcs-dds-api";
import { PREFERRED_GROUP } from "../constants";
import type { DisplaySettings } from "../displaySettings";
import { useDisplaySettings } from "../displaySettingsStore";
import { useDataGroups } from "../hooks/useDataGroups";
import { useStatusGroupSummary } from "../hooks/useStatusGroupSummary";
import { DcpLocationAccordion } from "./DcpLocationAccordion";
import { DcpMonLoadingSkeleton } from "./DcpMonLoadingSkeleton";
import { DisplaySettingsModal } from "./DisplaySettingsModal";
import { ErrorBoundary } from "./ErrorBoundary";
import { StatusLegend } from "./StatusLegend";
import { SummaryCards } from "./SummaryCards";
import { TimestampDisplay } from "./TimestampDisplay";

type StationEntry = [string, DcpSummary];
type DcpStatus = DcpSummary["status"];
type StatusSection = {
  eventKey: string;
  label: string;
  variant: string;
  stations: StationEntry[];
  badgeLabel?: string;
  badgeVariant?: string;
};

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

function StationList({
  stations,
  displaySettings,
  badgeLabel,
  badgeVariant,
}: {
  stations: StationEntry[];
  displaySettings: DisplaySettings;
  badgeLabel?: string;
  badgeVariant?: string;
}) {
  return stations.map(([dcpAddress, stationSummary]) => (
    <ErrorBoundary
      key={dcpAddress}
      resetKey={`${dcpAddress}-${stationSummary.status}-${stationSummary.messageTotal}-${stationSummary.expectedMessageTotal}`}
      fallback={
        <Alert variant="danger">
          Unable to display location {dcpAddress}.
        </Alert>
      }
    >
      <DcpLocationAccordion
        dcpAddress={dcpAddress}
        summary={stationSummary}
        displaySettings={displaySettings}
        badgeLabel={badgeLabel}
        badgeVariant={badgeVariant}
      />
    </ErrorBoundary>
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
  const [settingsOpen, setSettingsOpen] = useState(false);
  const { settings: displaySettings, saveSettings } = useDisplaySettings();
  const preferredGroup = groups.data?.find(
    (group) => group.id.toLowerCase() === PREFERRED_GROUP.toLowerCase(),
  );
  const selectedGroup = groups.data?.some((group) => group.id === requestedGroup)
    ? requestedGroup
    : (preferredGroup ?? groups.data?.[0])?.id ?? "";
  const summary = useStatusGroupSummary(selectedGroup);

  if (groups.isLoading || (selectedGroup && summary.isLoading)) {
    return (
      <>
        <DcpMonLoadingSkeleton
          groups={groups.data}
          selectedGroup={selectedGroup}
          onGroupChange={(group) => {
            setRequestedGroup(group);
            setSearchQuery("");
          }}
          onSettingsClick={() => setSettingsOpen(true)}
        />
        {settingsOpen && (
          <DisplaySettingsModal
            settings={displaySettings}
            onHide={() => setSettingsOpen(false)}
            onSave={(nextSettings) => {
              saveSettings(nextSettings);
              setSettingsOpen(false);
            }}
          />
        )}
      </>
    );
  }

  if (groups.isError) {
    return (
      <Stack gap={3}>
        <h1 className="h3 mb-0">DCPMon</h1>
        <Alert variant="danger" className="d-flex flex-wrap align-items-center justify-content-between gap-2">
          <span>Unable to load DCPMon groups.</span>
          <Button
            variant="outline-danger"
            size="sm"
            onClick={() => void groups.refetch()}
            disabled={groups.isFetching}
          >
            Try again
          </Button>
        </Alert>
      </Stack>
    );
  }

  if (!groups.data?.length) {
    return <Alert variant="warning">No DCP status groups are configured.</Alert>;
  }

  if (!selectedGroup) return null;

  if (summary.isError || !summary.data) {
    return (
      <Stack gap={3}>
        <Row className="dcpmon-dashboard-header align-items-end g-3">
          <Col xs={12} md>
            <h1 className="h3 mb-1">DCPMon</h1>
            <div className="text-secondary">Group {selectedGroup}</div>
          </Col>
          <Col xs md={3}>
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
        <Alert variant="danger" className="d-flex flex-wrap align-items-center justify-content-between gap-2">
          <span>Unable to load DCPMon status summary.</span>
          <Button
            variant="outline-danger"
            size="sm"
            onClick={() => void summary.refetch()}
            disabled={summary.isFetching}
          >
            Try again
          </Button>
        </Alert>
      </Stack>
    );
  }

  const stations = Object.entries(summary.data.dcpSummaries ?? {}) as StationEntry[];
  const normalizedSearch = searchQuery.trim().toLocaleLowerCase();
  const filteredStations = stations.filter((station) =>
    matchesSearch(station, normalizedSearch),
  );
  const statusSections: StatusSection[] = statusOrder.map((status) => ({
    eventKey: status,
    label: statusLabels[status],
    variant: statusVariants[status],
    stations: stations.filter(
      ([, stationSummary]) => stationSummary.status === status,
    ),
  }));
  const completeSection = statusSections.find(
    (section) => section.eventKey === "complete",
  )!;
  const conditionSections: StatusSection[] = [
    {
      eventKey: "low-battery",
      label: "Low battery",
      variant: "warning",
      stations: stations.filter(([, stationSummary]) => stationSummary.lowBattery),
      badgeLabel: "LOW BATTERY",
      badgeVariant: "warning",
    },
    {
      eventKey: "gps-sync",
      label: "GPS sync issues",
      variant: "gps-sync",
      stations: stations.filter(
        ([, stationSummary]) => stationSummary.gpsSync === false,
      ),
      badgeLabel: "GPS SYNC",
      badgeVariant: "gps-sync",
    },
  ].filter((section) => section.stations.length > 0);
  const visibleSections = [
    ...statusSections.filter(
      (section) =>
        section.eventKey !== "complete" &&
        (section.eventKey !== "unknown" || section.stations.length > 0),
    ),
    ...conditionSections,
    completeSection,
  ];
  const durationHours = summary.data.durationHours ?? 0;

  return (
    <Stack gap={4}>
      <Row className="dcpmon-dashboard-header align-items-end g-3">
        <Col xs={12} md>
          <h1 className="h3 mb-1">DCPMon</h1>
          <div className="text-secondary">
            Group {selectedGroup} for the last {durationHours} hours · {stations.length}{" "}
            locations
          </div>
          <div className="small text-secondary">
            Updated{" "}
            <TimestampDisplay
              value={summary.data.timestamp}
              settings={displaySettings}
            />
          </div>
        </Col>
        <Col xs="auto" md="auto">
          <Button
            variant="outline-secondary"
            className="dcpmon-settings-button d-flex align-items-center gap-2"
            onClick={() => setSettingsOpen(true)}
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

      {settingsOpen && (
        <DisplaySettingsModal
          settings={displaySettings}
          onHide={() => setSettingsOpen(false)}
          onSave={(nextSettings) => {
            saveSettings(nextSettings);
            setSettingsOpen(false);
          }}
        />
      )}

      <ErrorBoundary
        resetKey={`${selectedGroup}-${summary.data.timestamp.toISOString()}`}
        fallback={<Alert variant="danger">Unable to display summary counts.</Alert>}
      >
        <SummaryCards summary={summary.data} />
      </ErrorBoundary>

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
              placeholder="ID, description, or NESDIS address"
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
              <StationList
                stations={filteredStations}
                displaySettings={displaySettings}
              />
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
          defaultActiveKey={visibleSections
            .filter((section) =>
              ["missing", "parity", "partial", "unknown"].includes(
                section.eventKey,
              ),
            )
            .map((section) => section.eventKey)}
          className="dcpmon-status-sections"
        >
          {visibleSections.map((section) => (
              <Accordion.Item
                key={section.eventKey}
                eventKey={section.eventKey}
                className={`dcpmon-section-${section.eventKey}`}
              >
                <Accordion.Header>
                  <span className="d-flex align-items-center justify-content-between w-100 pe-3">
                    <span className="fw-semibold">{section.label}</span>
                    <Badge bg={section.variant}>
                      {section.stations.length} locations
                    </Badge>
                  </span>
                </Accordion.Header>
                <Accordion.Body>
                  <ErrorBoundary
                    resetKey={`${selectedGroup}-${section.eventKey}-${summary.data.timestamp.toISOString()}`}
                    fallback={
                      <Alert variant="danger" className="mb-0">
                        Unable to display this status category.
                      </Alert>
                    }
                  >
                    {section.stations.length ? (
                      <StationList
                        stations={section.stations}
                        displaySettings={displaySettings}
                        badgeLabel={section.badgeLabel}
                        badgeVariant={section.badgeVariant}
                      />
                    ) : (
                      <span className="text-secondary">No locations</span>
                    )}
                  </ErrorBoundary>
                </Accordion.Body>
              </Accordion.Item>
            ))}
        </Accordion>
      )}

      <ErrorBoundary
        resetKey={selectedGroup}
        fallback={<Alert variant="danger">Unable to display the status legend.</Alert>}
      >
        <StatusLegend />
      </ErrorBoundary>
    </Stack>
  );
}
