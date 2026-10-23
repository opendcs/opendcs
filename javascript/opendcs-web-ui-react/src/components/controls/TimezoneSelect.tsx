import { useId, useMemo } from "react";
import { Form } from "react-bootstrap";
import { getTimeZones, withTimeZone } from "./timezones";

interface TimezoneSelectProps {
  id?: string;
  value?: string;
  edit?: boolean;
  onChange: (value: string) => void;
  ariaLabel?: string;
  /**
   * Render a text input that suggests the zone list instead of a closed
   * select, so names outside it ("EST", "GMT-05:00" for a logger kept on
   * standard time) can still be typed. Only for fields the backend parses
   * leniently; routing's output zone goes through ZoneId.of and must stay
   * closed.
   */
  freeText?: boolean;
}

export const TimezoneSelect: React.FC<TimezoneSelectProps> = ({
  id,
  value,
  edit = true,
  onChange,
  ariaLabel,
  freeText = false,
}) => {
  const zones = useMemo(() => getTimeZones(), []);
  const listId = useId();

  if (freeText) {
    return (
      <>
        <Form.Control
          type="text"
          id={id}
          aria-label={ariaLabel}
          list={edit ? listId : undefined}
          readOnly={!edit}
          autoComplete="off"
          value={value ?? ""}
          onChange={(e) => onChange(e.currentTarget.value)}
        />
        {edit && (
          <datalist id={listId}>
            {zones.map((z) => (
              <option key={z} value={z} />
            ))}
          </datalist>
        )}
      </>
    );
  }

  return (
    <Form.Select
      id={id}
      aria-label={ariaLabel}
      value={value ?? ""}
      disabled={!edit}
      onChange={(e) => onChange(e.currentTarget.value)}
    >
      <option value="" />
      {withTimeZone(zones, value).map((z) => (
        <option key={z} value={z}>
          {z}
        </option>
      ))}
    </Form.Select>
  );
};
