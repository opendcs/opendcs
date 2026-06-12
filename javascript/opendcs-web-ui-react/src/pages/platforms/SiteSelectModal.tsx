import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiSiteRef } from "opendcs-api";
import { useSitesQuery } from "../../queries/sites";
import { SelectorModal, type ChooserColumnDef } from "../../components/data-table";
import { siteDisplayName } from "./siteDisplayName";

interface Props {
  show: boolean;
  onHide: () => void;
  onSelect: (site: ApiSiteRef) => void;
}

export const SiteSelectModal: React.FC<Props> = ({ show, onHide, onSelect }) => {
  const [t] = useTranslation(["platforms", "translation"]);
  const { data: sites = [], isFetching } = useSitesQuery();

  const columns: ChooserColumnDef<ApiSiteRef>[] = useMemo(
    () => [
      { data: "siteId", header: t("platforms:header.Id"), type: "num" },
      {
        data: null,
        header: t("platforms:public_name"),
        render: (_d, _t, row) => siteDisplayName(row),
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
    <SelectorModal<ApiSiteRef, number>
      show={show}
      onHide={onHide}
      onSelect={onSelect}
      title={t("platforms:select_site")}
      noneMessage={t("platforms:select_site_none")}
      data={sites}
      loading={isFetching}
      getId={(s) => s.siteId ?? -1}
      columns={columns}
    />
  );
};
