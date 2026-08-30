import { useState } from "react";
import Accordion from "react-bootstrap/Accordion";
import Alert from "react-bootstrap/Alert";
import Badge from "react-bootstrap/Badge";
import Button from "react-bootstrap/Button";
import Spinner from "react-bootstrap/Spinner";
import type { DcpSummary } from "opendcs-dds-api";
import type { DisplaySettings } from "../displaySettings";
import { useDcpMessages } from "../hooks/useDcpMessages";
import { DcpMessageTable } from "./DcpMessageTable";
import { ErrorBoundary } from "./ErrorBoundary";
import { StatusBadge } from "./StatusBadge";

type DcpLocationAccordionProps = {
  dcpAddress: string;
  summary: DcpSummary;
  displaySettings: DisplaySettings;
  badgeLabel?: string;
  badgeVariant?: string;
};

export function DcpLocationAccordion({
  dcpAddress,
  summary,
  displaySettings,
  badgeLabel,
  badgeVariant,
}: DcpLocationAccordionProps) {
  const [isOpen, setIsOpen] = useState(false);
  const messages = useDcpMessages(dcpAddress, isOpen);
  const preferredIdentifier =
    summary.identifiers?.find(
      (identifier) => identifier.type.toLowerCase() === "shef",
    ) ??
    summary.identifiers?.find(
      (identifier) => identifier.type.toLowerCase() === "local",
    ) ??
    summary.identifiers?.[0];

  return (
    <Accordion
      className="dcpmon-station-accordion mb-2"
      onSelect={(eventKey) => setIsOpen(eventKey === dcpAddress)}
    >
      <Accordion.Item eventKey={dcpAddress}>
        <Accordion.Header>
          <span className="dcpmon-station-heading">
            {badgeLabel ? (
              <Badge bg={badgeVariant}>{badgeLabel}</Badge>
            ) : (
              <StatusBadge status={summary.status} />
            )}
            <span className="dcpmon-station-address fw-semibold">
              {dcpAddress}
            </span>
            <span className="dcpmon-station-id text-secondary">
              {preferredIdentifier?.id ?? "No identifier"}
            </span>
            <span className="dcpmon-message-count text-secondary">
              {summary.messageTotal} / {summary.expectedMessageTotal ?? "?"}
            </span>
          </span>
        </Accordion.Header>
        <Accordion.Body>
          <dl className="row mb-3">
            <dt className="col-sm-3">Identifiers</dt>
            <dd className="col-sm-9">
              {summary.identifiers?.length
                ? summary.identifiers
                    .map((identifier) => `${identifier.type}: ${identifier.id}`)
                    .join(", ")
                : "None"}
            </dd>
            <dt className="col-sm-3">Parity Count</dt>
            <dd className="col-sm-9">{summary.parityCount}</dd>
          </dl>
          {messages.isLoading && (
            <div className="d-flex align-items-center gap-2 text-secondary">
              <Spinner animation="border" size="sm" />
              Loading GOES messages
            </div>
          )}
          {messages.isError && (
            <Alert variant="danger" className="d-flex flex-wrap align-items-center justify-content-between gap-2">
              <span>Error loading DCP message data.</span>
              <Button
                variant="outline-danger"
                size="sm"
                onClick={() => void messages.refetch()}
                disabled={messages.isFetching}
              >
                Try again
              </Button>
            </Alert>
          )}
          {messages.data && (
            <ErrorBoundary
              resetKey={`${dcpAddress}-${messages.data.messages?.length ?? 0}`}
              fallback={
                <Alert variant="danger" className="mb-0">
                  Unable to display message details for this location.
                </Alert>
              }
            >
              <DcpMessageTable
                messages={messages.data.messages ?? []}
                transmissionSlots={messages.data.transmissionSlots}
                displaySettings={displaySettings}
              />
            </ErrorBoundary>
          )}
        </Accordion.Body>
      </Accordion.Item>
    </Accordion>
  );
}
