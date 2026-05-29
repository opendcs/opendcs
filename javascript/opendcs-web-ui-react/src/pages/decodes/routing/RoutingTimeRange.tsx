import { useMemo } from "react";
import { Col, Form, FormGroup, Row } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import {
  SinceUntilEditor,
  type SinceUntilLabels,
} from "../../../components/controls/SinceUntilEditor";

const APPLY_TIME_TO_OPTIONS = [
  "Local Receive Time",
  "Platform Xmit Time",
  "Both",
] as const;

export interface RoutingTimeRangeProps {
  since: string | undefined;
  until: string | undefined;
  applyTimeTo: string | undefined;
  edit?: boolean;
  onChange: (field: "since" | "until" | "applyTimeTo", value: string) => void;
}

/**
 * The routing spec's time-window controls: the Since / Until builders plus the
 * "apply time to" reference selector. Extracted from the routing detail so the
 * search-criteria region stays readable.
 */
export const RoutingTimeRange: React.FC<RoutingTimeRangeProps> = ({
  since,
  until,
  applyTimeTo,
  edit = false,
  onChange,
}) => {
  const [t] = useTranslation(["routing"]);

  // Shared option/aria strings; only the method label differs per editor.
  const commonLabels = useMemo(
    () => ({
      now: t("routing:now"),
      nowMinus: t("routing:now_minus"),
      amount: t("routing:now_minus_amount"),
      calendar: t("routing:calendar"),
    }),
    [t],
  );
  const sinceLabels: SinceUntilLabels = { ...commonLabels, method: t("routing:since") };
  const untilLabels: SinceUntilLabels = { ...commonLabels, method: t("routing:until") };

  return (
    <>
      <FormGroup as={Row} className="mb-3">
        <Form.Label column sm={4}>
          {t("routing:since")}
        </Form.Label>
        <Col sm={8}>
          <SinceUntilEditor
            kind="since"
            value={since}
            edit={edit}
            labels={sinceLabels}
            onChange={(v) => onChange("since", v)}
          />
        </Col>
      </FormGroup>
      <FormGroup as={Row} className="mb-3">
        <Form.Label column sm={4}>
          {t("routing:until")}
        </Form.Label>
        <Col sm={8}>
          <SinceUntilEditor
            kind="until"
            value={until}
            edit={edit}
            labels={untilLabels}
            onChange={(v) => onChange("until", v)}
          />
        </Col>
      </FormGroup>
      <FormGroup as={Row} className="mb-3">
        <Form.Label column sm={4} htmlFor="applyTimeTo">
          {t("routing:apply_time_to")}
        </Form.Label>
        <Col sm={8}>
          <Form.Select
            id="applyTimeTo"
            aria-label={t("routing:apply_time_to")}
            value={applyTimeTo ?? APPLY_TIME_TO_OPTIONS[0]}
            disabled={!edit}
            onChange={(e) => onChange("applyTimeTo", e.currentTarget.value)}
          >
            {APPLY_TIME_TO_OPTIONS.map((opt) => (
              <option key={opt} value={opt}>
                {opt}
              </option>
            ))}
          </Form.Select>
        </Col>
      </FormGroup>
    </>
  );
};

export default RoutingTimeRange;
