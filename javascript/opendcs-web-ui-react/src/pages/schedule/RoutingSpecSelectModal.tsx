import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiRoutingRef } from "opendcs-api";
import { SelectorModal, type ChooserColumnDef } from "../../components/data-table";
import { useRoutingsQuery } from "../../queries/routing";

interface Props {
  show: boolean;
  onHide: () => void;
  onSelect: (routing: ApiRoutingRef) => void;
}

/**
 * Queries the routing list itself rather than taking it as a prop: Schedule
 * renders inside an AppDataTable detail row, which is built once per open and
 * never re-rendered with new props, so a list passed down while still loading
 * would stay empty (spinner stuck) for as long as the row is open (#2201).
 */
export const RoutingSpecSelectModal: React.FC<Props> = ({ show, onHide, onSelect }) => {
  const [t] = useTranslation(["schedule", "routing", "translation"]);
  const { data: routings = [], isFetching } = useRoutingsQuery();

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
      loading={isFetching}
      getId={(r) => r.routingId ?? -1}
      columns={columns}
    />
  );
};

export default RoutingSpecSelectModal;
