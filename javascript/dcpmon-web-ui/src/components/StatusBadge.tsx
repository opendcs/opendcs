import Badge from "react-bootstrap/Badge";
import type { DcpSummary } from "opendcs-dds-api";

type DcpStatus = DcpSummary["status"];

type StatusBadgeProps = {
  status: DcpStatus;
};

const variants: Record<DcpStatus, string> = {
  complete: "success",
  partial: "warning",
  parity: "info",
  missing: "danger",
  unknown: "secondary",
};

export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <Badge bg={variants[status]} className="text-uppercase">
      {status}
    </Badge>
  );
}
