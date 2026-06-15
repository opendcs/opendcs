import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import type { ApiNetlistRef } from "opendcs-api";
import {
  AppDataTable,
  type ColumnDef,
  type HeaderButton,
  type RowAction,
} from "../../../components/data-table";
import { NetlistsAddModal } from "./NetlistsAddModal";

export interface RoutingNetlistsTableProperties {
  /** All network lists available to attach (resolves details + populates modal). */
  allNetlists: ApiNetlistRef[];
  allNetlistsLoading?: boolean;
  /** Names of network lists currently attached to this routing spec. */
  selectedNetlistNames: string[];
  edit?: boolean;
  onAdd: (names: string[]) => void;
  onRemove: (name: string) => void;
}

interface SelectedRow {
  name: string;
  ref?: ApiNetlistRef;
}

export const RoutingNetlistsTable: React.FC<RoutingNetlistsTableProperties> = ({
  allNetlists,
  allNetlistsLoading = false,
  selectedNetlistNames,
  edit = false,
  onAdd,
  onRemove,
}) => {
  const [t] = useTranslation(["routing", "translation"]);
  const [showAddModal, setShowAddModal] = useState(false);

  const refsByName = useMemo(() => {
    const m = new Map<string, ApiNetlistRef>();
    for (const n of allNetlists) {
      if (n.name !== undefined) m.set(n.name, n);
    }
    return m;
  }, [allNetlists]);

  const rows = useMemo<SelectedRow[]>(
    () => selectedNetlistNames.map((name) => ({ name, ref: refsByName.get(name) })),
    [selectedNetlistNames, refsByName],
  );

  const columns = useMemo<ColumnDef<SelectedRow>[]>(
    () => [
      {
        data: null,
        header: t("routing:netlist_name"),
        defaultContent: "",
        render: (_d, _t, row) => row.name,
      },
      {
        data: null,
        header: t("routing:netlist_tm_type"),
        defaultContent: "",
        render: (_d, _t, row) => row.ref?.transportMediumType ?? "",
      },
      {
        data: null,
        header: t("routing:netlist_num_platforms"),
        defaultContent: "",
        type: "num",
        render: (_d, _t, row) => row.ref?.numPlatforms ?? "",
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<SelectedRow>[]>(() => {
    if (!edit) return [];
    return [
      {
        key: "remove",
        icon: "bi-trash",
        variant: "danger",
        aria: (row) => t("routing:remove_netlist", { name: row.name }),
        onClick: ({ row }) => onRemove(row.name),
      },
    ];
  }, [edit, t, onRemove]);

  const extraHeaderButtons = useMemo<HeaderButton[]>(() => {
    if (!edit) return [];
    return [
      {
        text: "+",
        ariaLabel: t("routing:add_netlists"),
        onClick: () => setShowAddModal(true),
      },
    ];
  }, [edit, t]);

  return (
    <>
      <AppDataTable<SelectedRow, string>
        data={rows}
        getId={(r) => r.name}
        columns={columns}
        actionsLabel={edit ? t("translation:actions") : undefined}
        rowActions={rowActions}
        extraHeaderButtons={extraHeaderButtons}
        caption={t("routing:netlists")}
        tableId="routingSelectedNetlistsTable"
      />
      <NetlistsAddModal
        show={showAddModal}
        onHide={() => setShowAddModal(false)}
        netlists={allNetlists}
        loading={allNetlistsLoading}
        alreadySelectedNames={selectedNetlistNames}
        onAdd={onAdd}
      />
    </>
  );
};

export default RoutingNetlistsTable;
