import type React from "react";
import { FormSelect } from "react-bootstrap";
import { useUnits } from "../../contexts/data/UnitsContext";

export interface UnitSelectorProperties {
  current?: string;
  onChange?: (event: React.ChangeEvent<HTMLSelectElement>) => void;
  disabled?: boolean;
}

const UnitSelect: React.FC<UnitSelectorProperties> = ({
  current,
  onChange,
  disabled,
}) => {
  const units = useUnits();
  console.log(`Will set default value to ${current}`);
  return units.ready ? (
    <FormSelect
      defaultValue={current}
      onChange={(e) => {
        e.preventDefault();
        onChange?.(e);
      }}
      disabled={disabled}
    >
      {Object.entries(units.units).map(([id, unit]) => {
        return (
          <option key={id} value={unit.abbr}>
            {unit.abbr}
          </option>
        );
      })}
    </FormSelect>
  ) : (
    <div>Loading</div>
  );
};

export default UnitSelect;
