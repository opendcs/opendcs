import { FormSelect } from "react-bootstrap";
import { REFLIST_SITE_NAME_TYPE, useRefList } from "../../contexts/data/RefListContext";
import { useTranslation } from "react-i18next";

export interface SiteNameTypeSelectProperties {
  defaultValue?: string;
}

export const SiteNameTypeSelect: React.FC<SiteNameTypeSelectProperties> = ({
  defaultValue: current,
}) => {
  const { refList } = useRefList();
  const { t } = useTranslation();
  const siteNameTypes = refList(REFLIST_SITE_NAME_TYPE);

  return (
    <FormSelect
      name="siteNameType"
      defaultValue={current}
      aria-label={t("sites:site_names.select")}
    >
      {siteNameTypes.items
        ? Object.values(siteNameTypes.items!).map((item) => {
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
