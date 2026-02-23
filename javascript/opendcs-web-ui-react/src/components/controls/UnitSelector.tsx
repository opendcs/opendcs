import type React from "react";
import { FormSelect } from "react-bootstrap";
import { useUnits } from "../../contexts/data/UnitsContext";

export interface UnitSelectorProperties {
  current?: string;
  onChange: () => void;
}

const UnitSelect: React.FC<UnitSelectorProperties> = ({ current, onChange }) => {
  const units = useUnits();

  return units.ready ? (
    <FormSelect defaultValue={current}>
      {Object.entries(units.units).map(([id, unit]) => {
        return (
          <option key={id} value={unit.name}>
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
