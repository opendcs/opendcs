import type React from "react";
import { FormSelect } from "react-bootstrap";
import {
  REFLIST_UNIT_CONVERSION_ALGORITHM,
  useRefList,
} from "../../contexts/data/RefListContext";

export interface UnitConversionAlgorithmSelectorProperties {
  current?: string;
  onChange?: (event: React.ChangeEvent<HTMLSelectElement>) => void;
  disabled?: boolean;
}

const UnitConversionAlgorithmSelect: React.FC<
  UnitConversionAlgorithmSelectorProperties
> = ({ current, onChange, disabled }) => {
  const { refList, ready } = useRefList();

  if (!ready) {
    return <div>Loading...</div>;
  }
  const algorithms = refList(REFLIST_UNIT_CONVERSION_ALGORITHM);

  return (
    <FormSelect
      defaultValue={current}
      onChange={(e) => {
        e.preventDefault();
        onChange?.(e);
      }}
      disabled={disabled}
    >
      {algorithms.items &&
        Object.values(algorithms.items).map((item) => {
          return (
            <option key={item.value} value={item.value}>
              {item.value}
            </option>
          );
        })}
    </FormSelect>
  );
};

export default UnitConversionAlgorithmSelect;
