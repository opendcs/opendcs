import Card from "react-bootstrap/Card";

const statusItems = [
  {
    label: "Complete",
    variant: "success",
    description: "All expected transmissions received with no parity failures.",
  },
  {
    label: "Partial",
    variant: "warning",
    description: "Some, but not all, expected transmissions were received.",
  },
  {
    label: "Parity",
    variant: "info",
    description: "Expected transmissions arrived, but at least one has a parity failure.",
  },
  {
    label: "Missing",
    variant: "danger",
    description: "No expected self-timed transmissions were received.",
  },
  {
    label: "Unknown",
    variant: "secondary",
    description: "A usable transmission schedule is not configured.",
  },
] as const;

export function StatusLegend() {
  return (
    <Card className="dcpmon-status-legend">
      <Card.Body>
        <div className="d-flex flex-wrap align-items-baseline gap-2 mb-3">
          <Card.Title as="h2" className="h6 mb-0">
            Status legend
          </Card.Title>
          <span className="small text-secondary">
            Based on the selected group’s 24-hour reporting window
          </span>
        </div>
        <div className="dcpmon-status-legend-grid">
          {statusItems.map(({ label, variant, description }) => (
            <div className="dcpmon-status-legend-item" key={label}>
              <span
                className={`dcpmon-status-swatch bg-${variant}`}
                aria-hidden="true"
              />
              <span>
                <span className="d-block small fw-semibold">{label}</span>
                <span className="d-block small text-secondary">
                  {description}
                </span>
              </span>
            </div>
          ))}
          <div className="dcpmon-status-legend-item dcpmon-status-legend-condition">
            <span
              className="dcpmon-status-swatch dcpmon-status-swatch-low-battery"
              aria-hidden="true"
            />
            <span>
              <span className="d-block small fw-semibold">Low battery</span>
              <span className="d-block small text-secondary">
                A received transmission reported the LRGS low-battery status.
              </span>
            </span>
          </div>
        </div>
      </Card.Body>
    </Card>
  );
}
