import { useMemo } from "react";
import { Col, Form, FormGroup, InputGroup, Row } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { TimezoneSelect } from "../decodes/routing/RoutingSelects";
import { formatRunInterval, parseRunInterval, type RunUnit } from "./runInterval";

export type RunMode = "continuously" | "once" | "every";

export type StartTimeInput = Date | string | undefined;

export interface ExecutionScheduleChange {
  startTime: Date | undefined;
  runInterval: string | undefined;
}

export interface ExecutionScheduleProps {
  startTime: StartTimeInput;
  runInterval: string | undefined;
  timeZone: string | undefined;
  edit?: boolean;
  /**
   * Fires with the next (startTime, runInterval) pair. Both arrive in the same
   * change so the parent reducer can update the API record atomically — the
   * three radio modes are encoded by the *combination* of these fields, so
   * splitting the callback would let the parent observe an inconsistent
   * intermediate state.
   */
  onChange: (next: ExecutionScheduleChange) => void;
  onTimeZoneChange: (value: string) => void;
}

// HTML <input type="datetime-local"> requires YYYY-MM-DDTHH:mm with no tz suffix.
const toLocalInput = (value: StartTimeInput): string => {
  if (!value) return "";
  const d = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(d.getTime())) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}`
  );
};

const fromLocalInput = (value: string): Date | undefined => {
  if (!value) return undefined;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? undefined : d;
};

// Mirrors the legacy Swing editor: no startTime means "run continuously"; a
// startTime with no interval means "run once"; both set means "run every".
const deriveMode = (
  startTime: StartTimeInput,
  runInterval: string | undefined,
): RunMode => {
  if (!startTime) return "continuously";
  return parseRunInterval(runInterval) ? "every" : "once";
};

const toDate = (value: StartTimeInput): Date | undefined => {
  if (value === undefined) return undefined;
  if (value instanceof Date) return value;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? undefined : d;
};

const UNITS: RunUnit[] = ["minute", "hour", "day"];

export const ExecutionSchedule: React.FC<ExecutionScheduleProps> = ({
  startTime,
  runInterval,
  timeZone,
  edit = false,
  onChange,
  onTimeZoneChange,
}) => {
  const [t] = useTranslation(["schedule"]);

  const mode = deriveMode(startTime, runInterval);
  const parsed = useMemo(() => parseRunInterval(runInterval), [runInterval]);
  // Carry the prior amount/unit forward when the user toggles modes, so
  // re-selecting "Periodically" doesn't blank the values they had typed.
  const count = parsed?.count ?? 1;
  const unit: RunUnit = parsed?.unit ?? "hour";

  const setMode = (next: RunMode) => {
    if (next === "continuously") {
      onChange({ startTime: undefined, runInterval: undefined });
      return;
    }
    // Switching to "once" or "periodically" requires a start time — default to
    // whatever the user had, falling back to "now" so the datetime-local input
    // has something to display.
    const baseStart = toDate(startTime) ?? new Date();
    onChange({
      startTime: baseStart,
      runInterval: next === "every" ? formatRunInterval({ count, unit }) : undefined,
    });
  };

  const setStart = (event: React.ChangeEvent<HTMLInputElement>) => {
    onChange({ startTime: fromLocalInput(event.target.value), runInterval });
  };

  const setCount = (event: React.ChangeEvent<HTMLInputElement>) => {
    const n = Number(event.target.value);
    const safe = Number.isFinite(n) && n >= 0 ? n : 0;
    onChange({
      startTime: toDate(startTime) ?? new Date(),
      runInterval: formatRunInterval({ count: safe, unit }),
    });
  };

  const setUnit = (event: React.ChangeEvent<HTMLSelectElement>) => {
    const nextUnit = (
      UNITS.includes(event.target.value as RunUnit) ? event.target.value : "hour"
    ) as RunUnit;
    onChange({
      startTime: toDate(startTime) ?? new Date(),
      runInterval: formatRunInterval({ count, unit: nextUnit }),
    });
  };

  const radioName = "schedule-run-mode";
  const startDisabled = !edit || mode === "continuously";
  const periodicDisabled = !edit || mode !== "every";

  return (
    <fieldset className="border rounded p-3">
      <legend className="float-none w-auto px-2 fs-6 text-muted">
        {t("schedule:execution_schedule")}
      </legend>

      <FormGroup className="mb-2">
        <Form.Check
          type="radio"
          id="run-mode-continuously"
          name={radioName}
          disabled={!edit}
          checked={mode === "continuously"}
          onChange={() => setMode("continuously")}
          label={t("schedule:run_continuously")}
        />
      </FormGroup>
      <FormGroup className="mb-2">
        <Form.Check
          type="radio"
          id="run-mode-once"
          name={radioName}
          disabled={!edit}
          checked={mode === "once"}
          onChange={() => setMode("once")}
          label={t("schedule:run_once")}
        />
      </FormGroup>
      <FormGroup className="mb-3">
        <Form.Check
          type="radio"
          id="run-mode-every"
          name={radioName}
          disabled={!edit}
          checked={mode === "every"}
          onChange={() => setMode("every")}
          label={t("schedule:run_every")}
        />
        <div className="ps-4 pt-2" style={{ maxWidth: "20rem" }}>
          <InputGroup size="sm">
            <Form.Control
              type="number"
              min={0}
              aria-label={t("schedule:run_amount")}
              disabled={periodicDisabled}
              value={parsed ? String(parsed.count) : ""}
              onChange={setCount}
            />
            <Form.Select
              aria-label={t("schedule:run_unit")}
              disabled={periodicDisabled}
              value={parsed ? parsed.unit : unit}
              onChange={setUnit}
            >
              {UNITS.map((u) => (
                <option key={u} value={u}>
                  {t(`schedule:unit_${u}`)}
                </option>
              ))}
            </Form.Select>
          </InputGroup>
        </div>
      </FormGroup>

      <FormGroup as={Row} className="mb-3">
        <Form.Label column sm={4} htmlFor="startTime">
          {t("schedule:start_time")}
        </Form.Label>
        <Col sm={8}>
          <Form.Control
            type="datetime-local"
            id="startTime"
            name="startTime"
            readOnly={startDisabled}
            disabled={startDisabled}
            value={toLocalInput(startTime)}
            onChange={setStart}
          />
        </Col>
      </FormGroup>

      <FormGroup as={Row} className="mb-0">
        <Form.Label column sm={4} htmlFor="timeZone">
          {t("schedule:timezone")}
        </Form.Label>
        <Col sm={8}>
          <TimezoneSelect
            id="timeZone"
            value={timeZone}
            edit={edit && mode !== "continuously"}
            ariaLabel={t("schedule:timezone")}
            onChange={onTimeZoneChange}
          />
        </Col>
      </FormGroup>
    </fieldset>
  );
};

export default ExecutionSchedule;
