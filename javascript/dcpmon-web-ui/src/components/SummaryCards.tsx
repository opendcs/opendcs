import Card from "react-bootstrap/Card";
import type { StatusGroupSummary } from "opendcs-dds-api";

type SummaryCardsProps = {
  summary: StatusGroupSummary;
};

export function SummaryCards({ summary }: SummaryCardsProps) {
  const cards = [
    ["Complete", summary.counts?.complete ?? 0, "text-bg-success"],
    ["Partial", summary.counts?.partial ?? 0, "text-bg-warning"],
    ["Parity", summary.counts?.parity ?? 0, "text-bg-info"],
    ["Missing", summary.counts?.missing ?? 0, "text-bg-danger"],
    ...((summary.counts?.unknown ?? 0) > 0
      ? [["Unknown", summary.counts?.unknown ?? 0, "text-bg-secondary"] as const]
      : []),
  ];

  return (
    <div className={`dcpmon-summary-grid dcpmon-summary-grid--${cards.length} mb-4`}>
      {cards.map(([label, value, className]) => (
        <Card key={label} className={`dcpmon-summary-card ${className}`}>
          <Card.Body>
            <div className="dcpmon-summary-label text-uppercase">{label}</div>
            <div className="dcpmon-summary-value fw-semibold">{value}</div>
          </Card.Body>
        </Card>
      ))}
    </div>
  );
}
