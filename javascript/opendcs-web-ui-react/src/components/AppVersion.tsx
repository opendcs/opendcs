import { useTranslation } from "react-i18next";
import { useVersionQuery } from "../queries/version";

export interface AppVersionProps {
  className?: string;
}

// Short SHA (7 chars) to match GitHub's own display convention, so testers
// can directly compare the deployed commit to a GitHub commit URL.
const shortHash = (commitHash: string) => commitHash.slice(0, 7);

export const AppVersion: React.FC<AppVersionProps> = ({ className }) => {
  const { t } = useTranslation();
  const { data, isSuccess } = useVersionQuery();

  if (!isSuccess) {
    return null;
  }

  return (
    <span className={className}>
      {t("version")} {data.version} ({shortHash(data.commitHash)})
    </span>
  );
};

export default AppVersion;
