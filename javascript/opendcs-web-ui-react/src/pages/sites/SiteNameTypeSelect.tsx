import { FormSelect } from "react-bootstrap";
import { REFLIST_SITE_NAME_TYPE, useRefList } from "../../contexts/data/RefListContext";
import { useTranslation } from "react-i18next";
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
  const { refList, ready } = useRefList();
  const { t } = useTranslation(["sites"]);
  const siteNameTypes = refList(REFLIST_SITE_NAME_TYPE);
  return ready ? (
    <FormSelect
      name="siteNameType"
      defaultValue={defaultValue}
      aria-label={t("sites:site_names.select")}
      onChange={onChange}
    >
      {siteNameTypes.items
        ? Object.values(siteNameTypes.items!)
            .filter((snt) => !existing.find((esnt) => snt.value === esnt.type))
            .map((item) => {
              return (
                <option key={item.value} value={item.value}>
                  {item.value}
                </option>
              );
            })
        : null}
    </FormSelect>
  ) : (
    <p>{t("loading_reference_lists")}</p>
  );
};

export default SiteNameTypeSelect;
