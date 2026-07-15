import Badge from "react-bootstrap/Badge";
import type { DcpStatus } from "../types";

type StatusBadgeProps = {
  status: DcpStatus;
};

const variants: Record<DcpStatus, string> = {
  complete: "success",
  partial: "warning",
  parity: "info",
  missing: "danger",
};

export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <Badge bg={variants[status]} className="text-uppercase">
      {status}
    </Badge>
  );
}
