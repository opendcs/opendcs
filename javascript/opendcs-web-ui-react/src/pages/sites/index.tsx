import { useTranslation } from "react-i18next";

export const Sites = () => {
  const [t] = useTranslation("sites")
  return <div>{t("sites:sitesTitle")}</div>;
};
