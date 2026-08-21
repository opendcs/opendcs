import type React from "react";
import { FormSelect } from "react-bootstrap";
import { useTranslation } from "react-i18next";

export interface RefListPlaceholderSelectProperties {
  id?: string;
  ariaLabel?: string;
  name?: string;
  /** Value already stored on the record, so it stays visible while the list is unavailable. */
  value?: string;
  /** The reference list request finished and failed; the list is not coming. */
  failed?: boolean;
}

/**
 * Stand-in for a reference list backed FormSelect while its data is unavailable.
 */
export const RefListPlaceholderSelect: React.FC<RefListPlaceholderSelectProperties> = ({
  id,
  ariaLabel,
  name,
  value,
  failed = false,
}) => {
  const { t } = useTranslation();
  const status = failed
    ? t("reference_lists_unavailable")
    : t("loading_reference_lists");
  const shortStatus = failed ? t("unavailable_short") : t("loading_short");

  return (
    <FormSelect
      id={id}
      name={name}
      disabled
      title={status}
      aria-label={ariaLabel ? `${ariaLabel} (${status})` : status}
      value={value ?? ""}
    >
      <option value={value ?? ""}>{value || shortStatus}</option>
    </FormSelect>
  );
};

export default RefListPlaceholderSelect;
