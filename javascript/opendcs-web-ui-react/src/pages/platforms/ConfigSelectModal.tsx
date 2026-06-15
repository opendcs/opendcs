import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiConfigRef } from "opendcs-api";
import { useConfigsQuery } from "../../queries/configs";
import { SelectorModal, type ChooserColumnDef } from "../../components/data-table";

interface Props {
  show: boolean;
  onHide: () => void;
  onSelect: (config: ApiConfigRef) => void;
}

export const ConfigSelectModal: React.FC<Props> = ({ show, onHide, onSelect }) => {
  const [t] = useTranslation(["platforms", "translation"]);
  const { data: configs = [], isFetching } = useConfigsQuery();

  const columns: ChooserColumnDef<ApiConfigRef>[] = useMemo(
    () => [
      { data: "configId", header: t("platforms:header.Id"), type: "num" },
      { data: "name", header: t("platforms:config") },
      {
        data: "numPlatforms",
        header: t("platforms:select_config_num_platforms"),
        type: "num",
        defaultContent: "0",
      },
      {
        data: "description",
        header: t("platforms:header.Description"),
        defaultContent: "",
      },
    ],
    [t],
  );

  return (
    <SelectorModal<ApiConfigRef, number>
      show={show}
      onHide={onHide}
      onSelect={onSelect}
      title={t("platforms:select_config")}
      noneMessage={t("platforms:select_config_none")}
      data={configs}
      loading={isFetching}
      getId={(c) => c.configId ?? -1}
      columns={columns}
    />
  );
};
