import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import type { ApiPlatformRef } from "opendcs-api";
import {
  AppDataTable,
  type ColumnDef,
  type HeaderButton,
  type RowAction,
} from "../../../components/data-table";
import { PlatformsAddModal } from "./PlatformsAddModal";

export interface RoutingPlatformsTableProperties {
  /** All platforms available to attach (resolves details + populates the modal). */
  allPlatforms: ApiPlatformRef[];
  allPlatformsLoading?: boolean;
  /** DCP addresses attached to this routing spec (sc:DCP_ADDRESS). */
  selectedPlatformIds: string[];
  /** Platform names attached to this routing spec (sc:DCP_NAME). */
  selectedPlatformNames: string[];
  edit?: boolean;
  onAddNames: (names: string[]) => void;
  onRemoveName: (name: string) => void;
  onRemoveId: (id: string) => void;
}

interface SelectedRow {
  key: string;
  kind: "name" | "address";
  value: string;
  ref?: ApiPlatformRef;
}

export const RoutingPlatformsTable: React.FC<RoutingPlatformsTableProperties> = ({
  allPlatforms,
  allPlatformsLoading = false,
  selectedPlatformIds,
  selectedPlatformNames,
  edit = false,
  onAddNames,
  onRemoveName,
  onRemoveId,
}) => {
  const [t] = useTranslation(["routing", "platforms", "translation"]);
  const [showAddModal, setShowAddModal] = useState(false);

  const refsByName = useMemo(() => {
    const m = new Map<string, ApiPlatformRef>();
    for (const p of allPlatforms) {
      if (p.name !== undefined) m.set(p.name, p);
    }
    return m;
  }, [allPlatforms]);

  // DCP addresses live in each platform's transportMedia values.
  const refsByAddress = useMemo(() => {
    const m = new Map<string, ApiPlatformRef>();
    for (const p of allPlatforms) {
      for (const addr of Object.values(p.transportMedia ?? {})) {
        if (addr) m.set(addr, p);
      }
    }
    return m;
  }, [allPlatforms]);

  const rows = useMemo<SelectedRow[]>(() => {
    const nameRows = selectedPlatformNames.map<SelectedRow>((value) => ({
      key: `name:${value}`,
      kind: "name",
      value,
      ref: refsByName.get(value),
    }));
    const addrRows = selectedPlatformIds.map<SelectedRow>((value) => ({
      key: `addr:${value}`,
      kind: "address",
      value,
      ref: refsByAddress.get(value),
    }));
    return [...nameRows, ...addrRows];
  }, [selectedPlatformNames, selectedPlatformIds, refsByName, refsByAddress]);

  const columns = useMemo<ColumnDef<SelectedRow>[]>(
    () => [
      {
        data: null,
        header: t("platforms:header.Id"),
        defaultContent: "",
        type: "num",
        render: (_d, _t, row) => row.ref?.platformId ?? "",
      },
      {
        data: null,
        header: t("platforms:header.Platform"),
        defaultContent: "",
        render: (_d, _t, row) =>
          row.ref?.name ?? (row.kind === "name" ? row.value : ""),
      },
      {
        data: null,
        header: t("platforms:header.Agency"),
        defaultContent: "",
        render: (_d, _t, row) => row.ref?.agency ?? "",
      },
      {
        data: null,
        header: t("platforms:header.TransportId"),
        defaultContent: "",
        render: (_d, _t, row) => {
          if (row.kind === "address") return row.value;
          return row.ref?.transportMedia
            ? Object.values(row.ref.transportMedia).join(", ")
            : "";
        },
      },
      {
        data: null,
        header: t("routing:selected_by"),
        defaultContent: "",
        render: (_d, _t, row) =>
          row.kind === "name"
            ? t("routing:selected_by_name")
            : t("routing:selected_by_address"),
      },
      {
        data: null,
        header: t("platforms:header.Config"),
        defaultContent: "",
        render: (_d, _t, row) => row.ref?.config ?? "",
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
        aria: (row) =>
          t("routing:remove_platform", { name: row.ref?.name ?? row.value }),
        onClick: ({ row }) => {
          if (row.kind === "name") onRemoveName(row.value);
          else onRemoveId(row.value);
        },
      },
    ];
  }, [edit, t, onRemoveName, onRemoveId]);

  const extraHeaderButtons = useMemo<HeaderButton[]>(() => {
    if (!edit) return [];
    return [
      {
        text: "+",
        ariaLabel: t("routing:add_platforms"),
        onClick: () => setShowAddModal(true),
      },
    ];
  }, [edit, t]);

  return (
    <>
      <AppDataTable<SelectedRow, string>
        data={rows}
        getId={(r) => r.key}
        columns={columns}
        actionsLabel={edit ? t("translation:actions") : undefined}
        rowActions={rowActions}
        extraHeaderButtons={extraHeaderButtons}
        caption={t("routing:platforms")}
        tableId="routingSelectedPlatformsTable"
      />
      <PlatformsAddModal
        show={showAddModal}
        onHide={() => setShowAddModal(false)}
        platforms={allPlatforms}
        loading={allPlatformsLoading}
        alreadySelectedNames={selectedPlatformNames}
        onAdd={onAddNames}
      />
    </>
  );
};

export default RoutingPlatformsTable;
