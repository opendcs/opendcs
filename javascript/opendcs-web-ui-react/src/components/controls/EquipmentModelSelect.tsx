import type React from "react";
import { useMemo } from "react";
import { FormSelect } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { useEquipmentRefsQuery } from "../../queries/equipment";
import { compareStrings } from "../../util/sort";

export interface EquipmentModelSelectProperties {
  id?: string;
  /** Selected equipment model ID; undefined means none is assigned. */
  value?: number;
  onChange?: (equipmentId: number | undefined) => void;
  disabled?: boolean;
}

/**
 * Picks one of the defined equipment models, or none.
 */
export const EquipmentModelSelect: React.FC<EquipmentModelSelectProperties> = ({
  id,
  value,
  onChange,
  disabled = false,
}) => {
  const { t } = useTranslation(["equipment", "translation"]);
  const { data: models, isSuccess, isError } = useEquipmentRefsQuery();

  const sortedModels = useMemo(
    () => [...(models ?? [])].sort((a, b) => compareStrings(a.name, b.name)),
    [models],
  );

  if (!isSuccess) {
    const status = isError
      ? t("translation:unavailable_short")
      : t("translation:loading_short");
    return (
      <FormSelect id={id} value="" title={status} disabled>
        <option value="">{status}</option>
      </FormSelect>
    );
  }

  return (
    <FormSelect
      id={id}
      value={value ?? ""}
      disabled={disabled}
      onChange={(e) => {
        const selected = e.currentTarget.value;
        onChange?.(selected === "" ? undefined : Number(selected));
      }}
    >
      <option value="">{t("equipment:no_equipment_model")}</option>
      {sortedModels.map((model) => (
        <option key={model.equipmentId} value={model.equipmentId}>
          {model.name}
        </option>
      ))}
    </FormSelect>
  );
};

export default EquipmentModelSelect;
