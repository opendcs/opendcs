import type React from "react";
import { FormSelect, type FormSelectProps } from "react-bootstrap";
import { useUnitListQuery } from "../../queries/units";

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
  const { data: units, isSuccess } = useUnitListQuery();
  return isSuccess ? (
    <FormSelect
      {...props}
      defaultValue={current}
      onChange={(e) => {
        e.preventDefault();
        onChange?.(e);
      }}
      disabled={disabled}
    >
      {Object.entries(units).map(([id, unit]) => {
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
