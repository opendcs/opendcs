import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiRoutingRef } from "opendcs-api";
import { SelectorModal, type ChooserColumnDef } from "../../components/data-table";

interface Props {
  show: boolean;
  onHide: () => void;
  onSelect: (routing: ApiRoutingRef) => void;
  routings: ApiRoutingRef[];
  loading?: boolean;
}

export const RoutingSpecSelectModal: React.FC<Props> = ({
  show,
  onHide,
  onSelect,
  routings,
  loading = false,
}) => {
  const [t] = useTranslation(["schedule", "routing", "translation"]);

  const columns: ChooserColumnDef<ApiRoutingRef>[] = useMemo(
    () => [
      { data: "routingId", header: t("routing:header.Id"), type: "num" },
      { data: "name", header: t("routing:header.Name") },
      {
        data: "dataSourceName",
        header: t("routing:header.DataSource"),
        defaultContent: "",
      },
      {
        data: "destination",
        header: t("routing:header.Consumer"),
        defaultContent: "",
      },
    ],
    [t],
  );

  return (
    <SelectorModal<ApiRoutingRef, number>
      show={show}
      onHide={onHide}
      onSelect={onSelect}
      title={t("schedule:select_routing")}
      noneMessage={t("schedule:select_routing_none")}
      data={routings}
      loading={loading}
      getId={(r) => r.routingId ?? -1}
      columns={columns}
    />
  );
};

export default RoutingSpecSelectModal;
