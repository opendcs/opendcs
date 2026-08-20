import { FormSelect } from "react-bootstrap";
import { REFLIST_SITE_NAME_TYPE, useRefList } from "../../contexts/data/RefListContext";
import { useTranslation } from "react-i18next";
import { RefListPlaceholderSelect } from "../../components/controls/RefListPlaceholderSelect";
import type { SiteNameType } from "./SiteNameList";

export interface SiteNameTypeSelectProperties {
  defaultValue?: string;
  onChange?: (event: React.ChangeEvent<HTMLSelectElement>) => void;
  existing?: Partial<SiteNameType>[];
}

export const SiteNameTypeSelect: React.FC<SiteNameTypeSelectProperties> = ({
  defaultValue,
  onChange,
  existing = [],
}) => {
  const { refList, ready, failed } = useRefList();
  const { t, i18n } = useTranslation(["sites", "translation"]);
  const siteNameTypes = refList(REFLIST_SITE_NAME_TYPE);

  // Keep the dropdown in place, disabled, rather than collapsing it to text.
  if (!ready) {
    return (
      <RefListPlaceholderSelect
        name="siteNameType"
        ariaLabel={t("sites:site_names.select")}
        value={defaultValue}
        failed={failed}
      />
    );
  }

  return (
    <FormSelect
      key={i18n.language}
      name="siteNameType"
      defaultValue={defaultValue}
      aria-label={t("sites:site_names.select")}
      onChange={onChange}
    >
      {siteNameTypes.items
        ? Object.values(siteNameTypes.items)
            .filter((snt) => !existing.some((esnt) => snt.value === esnt.type))
            .map((item) => {
              return (
                <option key={item.value} value={item.value}>
                  {item.value}
                </option>
              );
            })
        : null}
    </FormSelect>
  );
};

export default SiteNameTypeSelect;
