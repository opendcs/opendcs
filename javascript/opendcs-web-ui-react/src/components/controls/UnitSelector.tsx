import type React from "react";
import { FormSelect, type FormSelectProps } from "react-bootstrap";
import { useUnitListQuery } from "../../queries/units";

export interface UnitSelectorProperties extends FormSelectProps {
  current?: string;
  onChange?: (event: React.ChangeEvent<HTMLSelectElement>) => void;
  disabled?: boolean;
}

// The decoder defaults `toAbbr` to "raw" for unconverted sensors. Mirror
// that default in the UI so an unset value displays as "raw" instead of a
// transient "Loading" placeholder.
const RAW_ABBR = "raw";

const UnitSelect: React.FC<UnitSelectorProperties> = ({
  current,
  onChange,
  disabled,
  ...props
}) => {
  const { data: units, isSuccess } = useUnitListQuery();
  const selected = current ?? RAW_ABBR;

  if (!isSuccess) {
    // Render a disabled select instead of swapping layout while the units
    // list loads (or never resolves). Shows the selected value (or "raw").
    return (
      <FormSelect {...props} value={selected} onChange={() => undefined} disabled>
        <option value={selected}>{selected}</option>
      </FormSelect>
    );
  }

  // When the units list is missing "raw" (older APIs / partial data), inject
  // it so the default value always has a matching option.
  const hasRaw = Object.values(units).some((u) => u.abbr === RAW_ABBR);

  return (
    <FormSelect
      {...props}
      defaultValue={selected}
      onChange={(e) => {
        e.preventDefault();
        onChange?.(e);
      }}
      disabled={disabled}
    >
      {!hasRaw && <option value={RAW_ABBR}>{RAW_ABBR}</option>}
      {Object.entries(units).map(([id, unit]) => (
        <option key={id} value={unit.abbr}>
          {unit.abbr}
        </option>
      ))}
    </FormSelect>
  );
};

export default UnitSelect;
