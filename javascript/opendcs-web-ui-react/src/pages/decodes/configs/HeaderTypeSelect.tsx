import React from "react";
import {
  REFLIST_DECODES_TRANSPORT_MEDIUM_TYPE,
  useRefList,
} from "../../../contexts/data/RefListContext";
import { FormSelect } from "react-bootstrap";
import { useTranslation } from "react-i18next";

interface HeaderSelectProperties {
  id?: string;
  defaultValue?: string;
  onChange?: (event: React.ChangeEvent<HTMLSelectElement>) => void;
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
  const { t } = useTranslation();
  const { refList, ready } = useRefList();

  const headerTypes = refList(REFLIST_DECODES_TRANSPORT_MEDIUM_TYPE);

  return ready ? (
    <FormSelect
      defaultValue={defaultValue}
      name="decodesHeaderType"
      onChange={onChange}
      disabled={!edit}
      id={id}
      aria-label={t("decodes:config.transport_medium_select")}
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
  ) : (
    <p>{t("loading_reference_lists")}</p>
  );
};
