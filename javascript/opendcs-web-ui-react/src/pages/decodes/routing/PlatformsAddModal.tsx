import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiPlatformRef } from "opendcs-api";
import {
  MultiSelectorModal,
  type ChooserColumnDef,
} from "../../../components/data-table";

export interface PlatformsAddModalProps {
  show: boolean;
  onHide: () => void;
  /** All available platforms (the modal filters out already-selected). */
  platforms: ApiPlatformRef[];
  loading?: boolean;
  /** Names of platforms already attached to the routing spec. */
  alreadySelectedNames: string[];
  onAdd: (names: string[]) => void;
}

export const PlatformsAddModal: React.FC<PlatformsAddModalProps> = ({
  show,
  onHide,
  platforms,
  loading = false,
  alreadySelectedNames,
  onAdd,
}) => {
  const [t] = useTranslation(["routing", "platforms", "translation"]);

  const columns = useMemo<ChooserColumnDef<ApiPlatformRef>[]>(
    () => [
      { data: "platformId", header: t("platforms:header.Id"), type: "num" },
      { data: "name", header: t("platforms:header.Platform") },
      {
        data: "agency",
        header: t("platforms:header.Agency"),
        defaultContent: "",
      },
      {
        data: null,
        header: t("platforms:header.TransportId"),
        defaultContent: "",
        render: (_d, _t, row) =>
          row.transportMedia ? Object.values(row.transportMedia).join(", ") : "",
      },
      {
        data: "config",
        header: t("platforms:header.Config"),
        defaultContent: "",
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
    <MultiSelectorModal<ApiPlatformRef, string>
      show={show}
      onHide={onHide}
      onAdd={onAdd}
      title={t("routing:add_platforms")}
      noneMessage={t("routing:add_platforms_none")}
      confirmLabel={(count) => t("routing:add_selected", { count })}
      data={platforms}
      loading={loading}
      getId={(p) => p.name ?? ""}
      columns={columns}
      excludeIds={alreadySelectedNames}
      selectAllAriaLabel={t("routing:select_all_platforms")}
      rowSelectAriaLabel={(p) =>
        t("routing:select_platform", { name: p.name ?? p.platformId })
      }
    />
  );
};

export default PlatformsAddModal;
