import { useCallback, useState } from "react";
import { Form, InputGroup } from "react-bootstrap";
import {
  dateToInputValue,
  formatAbsolute,
  inputValueToDate,
  isNow,
  parseAbsolute,
  parseNowMinus,
} from "./idTime";

type Method = "noLimit" | "now" | "nowMinus" | "calendar";

// Common now-minus amounts offered as suggestions; the field stays editable so
// custom amounts (e.g. "3 hours") are allowed, matching the Swing combo.
const AMOUNT_SUGGESTIONS = ["30 minutes", "1 hour", "2 hours", "1 day"];

const DEFAULT_AMOUNT = "1 hour";

/** Display strings, supplied by the caller so this control stays i18n-agnostic. */
export interface SinceUntilLabels {
  /** Accessible label for the method `<select>` (e.g. "Since" / "Until"). */
  method: string;
  /** "No Limit" option text (only shown when `allowNoLimit` is true). */
  noLimit?: string;
  /** "Now" option text (only shown when `kind === "until"`). */
  now: string;
  /** "Now minus" option text. */
  nowMinus: string;
  /** Accessible label for the now-minus amount input. */
  amount: string;
  /** "Calendar" option text + accessible label for the datetime input. */
  calendar: string;
}

export interface SinceUntilEditorProps {
  /** "since" omits the bare "now" option; "until" includes it. */
  kind: "since" | "until";
  value: string | undefined;
  edit?: boolean;
  onChange: (value: string) => void;
  labels: SinceUntilLabels;
  idPrefix?: string;
  /** When true, adds a "No Limit" option that emits an empty string. */
  allowNoLimit?: boolean;
}

interface ParsedState {
  method: Method;
  amount: string;
  dateInput: string;
}

const parseValue = (
  kind: "since" | "until",
  value: string | undefined,
  allowNoLimit: boolean,
): ParsedState => {
  if (allowNoLimit && !value) {
    return { method: "noLimit", amount: DEFAULT_AMOUNT, dateInput: "" };
  }
  if (kind === "until" && isNow(value)) {
    return { method: "now", amount: DEFAULT_AMOUNT, dateInput: "" };
  }
  const amount = parseNowMinus(value);
  if (amount !== undefined) {
    return { method: "nowMinus", amount, dateInput: "" };
  }
  const abs = parseAbsolute(value);
  if (abs) {
    return {
      method: "calendar",
      amount: DEFAULT_AMOUNT,
      dateInput: dateToInputValue(abs),
    };
  }
  // No usable value (blank or the buggy "now - null"): sensible default.
  if (allowNoLimit) {
    return { method: "noLimit", amount: DEFAULT_AMOUNT, dateInput: "" };
  }
  return kind === "until"
    ? { method: "now", amount: DEFAULT_AMOUNT, dateInput: "" }
    : { method: "nowMinus", amount: DEFAULT_AMOUNT, dateInput: "" };
};

const assemble = (state: ParsedState): string => {
  switch (state.method) {
    case "noLimit":
      return "";
    case "now":
      return "now";
    case "nowMinus":
      return `now - ${state.amount.trim() || DEFAULT_AMOUNT}`;
    case "calendar": {
      const d = inputValueToDate(state.dateInput);
      return d ? formatAbsolute(d) : "";
    }
  }
};

export const SinceUntilEditor: React.FC<SinceUntilEditorProps> = ({
  kind,
  value,
  edit = false,
  onChange,
  labels,
  idPrefix = kind,
  allowNoLimit = false,
}) => {
  const [state, setState] = useState<ParsedState>(() =>
    parseValue(kind, value, allowNoLimit),
  );

  const update = useCallback(
    (patch: Partial<ParsedState>) => {
      setState((prev) => {
        const next = { ...prev, ...patch };
        onChange(assemble(next));
        return next;
      });
    },
    [onChange],
  );

  const datalistId = `${idPrefix}-amounts`;

  return (
    <InputGroup>
      <Form.Select
        aria-label={labels.method}
        id={`${idPrefix}-method`}
        value={state.method}
        disabled={!edit}
        onChange={(e) => update({ method: e.currentTarget.value as Method })}
        style={{ maxWidth: "11rem" }}
      >
        {allowNoLimit && (
          <option value="noLimit">{labels.noLimit ?? "No Limit"}</option>
        )}
        {kind === "until" && <option value="now">{labels.now}</option>}
        <option value="nowMinus">{labels.nowMinus}</option>
        <option value="calendar">{labels.calendar}</option>
      </Form.Select>

      {state.method === "nowMinus" && (
        <>
          <Form.Control
            type="text"
            list={datalistId}
            aria-label={labels.amount}
            id={`${idPrefix}-amount`}
            value={state.amount}
            readOnly={!edit}
            onChange={(e) => update({ amount: e.currentTarget.value })}
          />
          <datalist id={datalistId}>
            {AMOUNT_SUGGESTIONS.map((a) => (
              <option key={a} value={a} />
            ))}
          </datalist>
        </>
      )}

      {state.method === "calendar" && (
        <Form.Control
          type="datetime-local"
          aria-label={labels.calendar}
          id={`${idPrefix}-calendar`}
          value={state.dateInput}
          readOnly={!edit}
          onChange={(e) => update({ dateInput: e.currentTarget.value })}
        />
      )}
    </InputGroup>
  );
};

export default SinceUntilEditor;
