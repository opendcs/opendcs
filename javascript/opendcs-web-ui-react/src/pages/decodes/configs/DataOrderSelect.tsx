import React, { useMemo } from "react";
import { FormSelect } from "react-bootstrap";
import { useTranslation } from "react-i18next";

interface DataOrderSelectProperties {
  id?: string;
  defaultValue?: string;
  onChange?: (event: React.ChangeEvent<HTMLSelectElement>) => void;
  edit?: boolean;
}

/**
 * Renders a FormSelect with the available Data Ordering values.
 * @param defaultValue what value should be initially shown as selected
 * @param onChange method to call to handle when the form select is changed
 * @param edit Can the user change the value
 * @returns
 */
export const DataOrderSelect: React.FC<DataOrderSelectProperties> = ({
  id,
  defaultValue,
  onChange,
  edit = true,
}) => {
  const { t } = useTranslation();

  const values = useMemo(() => {
    return {
      A: t("Ascending"),
      D: t("Descending"),
      U: t("Undefined"),
    };
  }, [t]);

  return (
    <FormSelect
      defaultValue={defaultValue}
      name="dataOrder"
      onChange={onChange}
      disabled={!edit}
      id={id}
      aria-label={t("decodes:data_order")}
    >
      {Object.entries(values).map((v) => {
        return (
          <option key={v[0]} value={v[0]}>
            {v[1]}
          </option>
        );
      })}
    </FormSelect>
  );
};
