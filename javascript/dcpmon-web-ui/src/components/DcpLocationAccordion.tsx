import { useState } from "react";
import Accordion from "react-bootstrap/Accordion";
import Alert from "react-bootstrap/Alert";
import Spinner from "react-bootstrap/Spinner";
import Stack from "react-bootstrap/Stack";
import { useDcpMessages } from "../hooks/useDcpMessages";
import type { DcpLocation } from "../types";
import { DcpMessageTable } from "./DcpMessageTable";
import { StatusBadge } from "./StatusBadge";

type DcpLocationAccordionProps = {
  location: DcpLocation;
  totalHours: number;
};

export function DcpLocationAccordion({
  location,
  totalHours,
}: DcpLocationAccordionProps) {
  const [isOpen, setIsOpen] = useState(false);
  const messages = useDcpMessages(location.dcpAddress, isOpen);

  return (
    <Accordion
      className="mb-2"
      onSelect={(eventKey) => setIsOpen(eventKey === location.dcpAddress)}
    >
      <Accordion.Item eventKey={location.dcpAddress}>
        <Accordion.Header>
          <Stack direction="horizontal" gap={3} className="w-100 me-3">
            <StatusBadge status={location.status} />
            <span className="fw-semibold">{location.dcpAddress}</span>
            <span className="text-secondary">{location.stationId}</span>
            <span className="ms-auto text-secondary">
              {location.messagesTotal} / {totalHours}
            </span>
          </Stack>
        </Accordion.Header>
        <Accordion.Body>
          <dl className="row mb-3">
            <dt className="col-sm-3">Location</dt>
            <dd className="col-sm-9">{location.locationCode}</dd>
            <dt className="col-sm-3">Parity Count</dt>
            <dd className="col-sm-9">{location.parityCount}</dd>
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
          {messages.data && <DcpMessageTable messages={messages.data.messages} />}
        </Accordion.Body>
      </Accordion.Item>
    </Accordion>
  );
}
