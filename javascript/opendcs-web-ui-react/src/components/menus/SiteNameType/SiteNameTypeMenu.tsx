import { Button, Dropdown } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import {
  SITE_NAME_TYPES,
  useSiteNameType,
  type SiteNameTypePreference,
} from "../../../contexts/app/SiteNameTypeContext";

interface ToggleProperties {
  label: string;
}

const SiteNameTypeToggle: React.FC<ToggleProperties> = ({ label, ...args }) => (
  <Button {...args}>
    <i className="bi bi-signpost-split me-1" aria-hidden="true" /> {label}
  </Button>
);

export const SiteNameTypeMenu = () => {
  const { siteNameType, setSiteNameType } = useSiteNameType();
  const [t] = useTranslation("sitenametype");

  const setPreferred = (preferredType: SiteNameTypePreference) =>
    setSiteNameType({ preferredType });

  const currentLabel = siteNameType.preferredType
    ? t(siteNameType.preferredType)
    : t("default");

  return (
    <Dropdown drop="start">
      <Dropdown.Toggle
        as={SiteNameTypeToggle}
        id="site-name-type"
        label={currentLabel}
        aria-label={t("dropdownLabel")}
      />
      <Dropdown.Menu>
        <Dropdown.Header>{t("menuTitle")}</Dropdown.Header>
        <Dropdown.Item
          active={siteNameType.preferredType === ""}
          onClick={() => setPreferred("")}
          aria-label={t("activate", { type: t("default") })}
        >
          {t("default")}
        </Dropdown.Item>
        <Dropdown.Divider />
        {SITE_NAME_TYPES.map((type) => (
          <Dropdown.Item
            key={type}
            active={siteNameType.preferredType === type}
            onClick={() => setPreferred(type)}
            aria-label={t("activate", { type: t(type) })}
          >
            {t(type)}
          </Dropdown.Item>
        ))}
      </Dropdown.Menu>
    </Dropdown>
  );
};

export default SiteNameTypeMenu;
