import { useState } from "react";
import Accordion from "react-bootstrap/Accordion";
import Alert from "react-bootstrap/Alert";
import Spinner from "react-bootstrap/Spinner";
import type { DcpSummary } from "opendcs-dds-api";
import { useDcpMessages } from "../hooks/useDcpMessages";
import { DcpMessageTable } from "./DcpMessageTable";
import { StatusBadge } from "./StatusBadge";

type DcpLocationAccordionProps = {
  dcpAddress: string;
  summary: DcpSummary;
  totalHours: number;
};

export function DcpLocationAccordion({
  dcpAddress,
  summary,
  totalHours,
}: DcpLocationAccordionProps) {
  const [isOpen, setIsOpen] = useState(false);
  const messages = useDcpMessages(dcpAddress, isOpen);
  const preferredIdentifier =
    summary.identifiers?.find(
      (identifier) => identifier.type.toLowerCase() === "shef",
    ) ?? summary.identifiers?.[0];

  return (
    <Accordion
      className="dcpmon-station-accordion mb-2"
      onSelect={(eventKey) => setIsOpen(eventKey === dcpAddress)}
    >
      <Accordion.Item eventKey={dcpAddress}>
        <Accordion.Header>
          <span className="dcpmon-station-heading">
            <StatusBadge status={summary.status} />
            <span className="dcpmon-station-address fw-semibold">
              {dcpAddress}
            </span>
            <span className="dcpmon-station-id text-secondary">
              {preferredIdentifier?.id ?? "No identifier"}
            </span>
            <span className="dcpmon-message-count text-secondary">
              {summary.messageTotal} / {totalHours}
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
            <Alert variant="danger">Error loading DCP message data.</Alert>
          )}
          {messages.data && (
            <DcpMessageTable messages={messages.data.messages ?? []} />
          )}
        </Accordion.Body>
      </Accordion.Item>
    </Accordion>
  );
}
