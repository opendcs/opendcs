import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiNetlistRef } from "opendcs-api";
import {
  MultiSelectorModal,
  type ChooserColumnDef,
} from "../../../components/data-table";

export interface NetlistsAddModalProps {
  show: boolean;
  onHide: () => void;
  /** All available network lists (the modal filters out already-selected). */
  netlists: ApiNetlistRef[];
  loading?: boolean;
  /** Names of network lists already attached to the routing spec. */
  alreadySelectedNames: string[];
  onAdd: (names: string[]) => void;
}

export const NetlistsAddModal: React.FC<NetlistsAddModalProps> = ({
  show,
  onHide,
  netlists,
  loading = false,
  alreadySelectedNames,
  onAdd,
}) => {
  const [t] = useTranslation(["routing", "translation"]);

  const columns = useMemo<ChooserColumnDef<ApiNetlistRef>[]>(
    () => [
      { data: "name", header: t("routing:netlist_name") },
      {
        data: "transportMediumType",
        header: t("routing:netlist_tm_type"),
        defaultContent: "",
      },
      {
        data: "numPlatforms",
        header: t("routing:netlist_num_platforms"),
        type: "num",
        defaultContent: "0",
      },
    ],
    [t],
  );

  return (
    <MultiSelectorModal<ApiNetlistRef, string>
      show={show}
      onHide={onHide}
      onAdd={onAdd}
      title={t("routing:add_netlists")}
      noneMessage={t("routing:add_netlists_none")}
      confirmLabel={(count) => t("routing:add_selected_netlists", { count })}
      data={netlists}
      loading={loading}
      getId={(n) => n.name ?? ""}
      columns={columns}
      excludeIds={alreadySelectedNames}
      size="lg"
      selectAllAriaLabel={t("routing:select_all_netlists")}
      rowSelectAriaLabel={(n) => t("routing:select_netlist", { name: n.name })}
    />
  );
};

export default NetlistsAddModal;
