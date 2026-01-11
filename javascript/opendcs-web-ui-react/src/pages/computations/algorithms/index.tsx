import { useTranslation } from "react-i18next";

export const Algorithms = () => {
  const [t] = useTranslation("algorithms");
  return <div>{t("algorithms:algorithmsTitle")}</div>;
};
