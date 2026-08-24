import React from "react";
import {
  REFLIST_DECODES_TRANSPORT_MEDIUM_TYPE,
  useRefList,
} from "../../../contexts/data/RefListContext";
import { FormSelect } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { RefListPlaceholderSelect } from "../../../components/controls/RefListPlaceholderSelect";

interface HeaderSelectProperties {
  id?: string;
  defaultValue?: string;
  onChange?: (header: string) => void;
  edit?: boolean;
}

/**
 * Renders a FormSelect with the available TransportMediumTypes in this systems.
 * @param defaultValue what value should be initially shown as selected
 * @param onChange method to call to handle when the form select is changed
 * @param edit Can the user change the value
 * @returns
 */
export const DecodesHeaderTypeSelect: React.FC<HeaderSelectProperties> = ({
  id,
  defaultValue,
  onChange,
  edit = true,
}) => {
  const { t } = useTranslation(["decodes", "translation"]);
  const { refList, ready, failed } = useRefList();

  const headerTypes = refList(REFLIST_DECODES_TRANSPORT_MEDIUM_TYPE);
  const label = t("decodes:config.transport_medium_select");

  // No list yet: hold the dropdown's place disabled rather than replacing it
  // with a status line, which pushed the control out of the row entirely.
  if (!ready) {
    return (
      <RefListPlaceholderSelect
        id={id}
        name="decodesHeaderType"
        ariaLabel={label}
        value={defaultValue}
        failed={failed}
      />
    );
  }

  return (
    <FormSelect
      defaultValue={defaultValue}
      name="decodesHeaderType"
      onChange={(e) => {
        onChange?.(e.currentTarget.value);
      }}
      disabled={!edit}
      id={id}
      aria-label={label}
    >
      {headerTypes.items &&
        Object.values(headerTypes.items).map((ht) => {
          return (
            <option key={ht.value} value={ht.value}>
              {ht.value}
            </option>
          );
        })}
    </FormSelect>
  );
};
