import Card from "react-bootstrap/Card";
import Col from "react-bootstrap/Col";
import Row from "react-bootstrap/Row";
import type { StatusGroupSummary } from "../types";

type SummaryCardsProps = {
  summary: StatusGroupSummary;
};

export function SummaryCards({ summary }: SummaryCardsProps) {
  const cards = [
    ["Complete", summary.summary.completeCount, "text-bg-success"],
    ["Partial", summary.summary.partialCount, "text-bg-warning"],
    ["Parity", summary.summary.parityCount, "text-bg-info"],
    ["Missing", summary.summary.missingCount, "text-bg-danger"],
  ] as const;

  return (
    <Row className="g-3 mb-4">
      {cards.map(([label, value, className]) => (
        <Col key={label} xs={6} lg={3}>
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
