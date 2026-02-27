import type React from "react";
import { FormSelect, type FormSelectProps } from "react-bootstrap";
import { useUnits } from "../../contexts/data/UnitsContext";

export interface UnitSelectorProperties extends FormSelectProps {
  current?: string;
  onChange?: (event: React.ChangeEvent<HTMLSelectElement>) => void;
  disabled?: boolean;
}

const UnitSelect: React.FC<UnitSelectorProperties> = ({
  current,
  onChange,
  disabled,
  ...props
}) => {
  const units = useUnits();
  return units.ready ? (
    <FormSelect
      {...props}
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
