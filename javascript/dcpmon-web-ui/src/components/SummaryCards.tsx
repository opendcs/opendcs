import Card from "react-bootstrap/Card";
import Col from "react-bootstrap/Col";
import Row from "react-bootstrap/Row";
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
    ["Unknown", summary.counts?.unknown ?? 0, "text-bg-secondary"],
  ] as const;

  return (
    <Row className="g-3 mb-4">
      {cards.map(([label, value, className]) => (
        <Col key={label} xs={6} lg>
          <Card className={`dcpmon-summary-card ${className}`}>
            <Card.Body>
              <div className="small text-uppercase">{label}</div>
              <div className="fs-3 fw-semibold">{value}</div>
            </Card.Body>
          </Card>
        </Col>
      ))}
    </Row>
  );
}
