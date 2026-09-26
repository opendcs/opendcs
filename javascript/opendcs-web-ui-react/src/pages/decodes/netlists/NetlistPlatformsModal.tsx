import { useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";
import {
  MultiSelectorModal,
  type ChooserColumnDef,
} from "../../../components/data-table";
import type { PlatformCandidate } from "./netlistPlatforms";

export interface NetlistPlatformsModalProps {
  show: boolean;
  onHide: () => void;
  /** Platforms with a medium of the netlist's type. */
  candidates: PlatformCandidate[];
  loading?: boolean;
  /** The netlist's transport medium type; the chooser is empty without one. */
  mediumType?: string;
  /** Transport ids already on the netlist — filtered out of the chooser. */
  existingTransportIds: string[];
  onAdd: (selected: PlatformCandidate[]) => void;
}

export const NetlistPlatformsModal: React.FC<NetlistPlatformsModalProps> = ({
  show,
  onHide,
  candidates,
  loading = false,
  mediumType,
  existingTransportIds,
  onAdd,
}) => {
  const [t] = useTranslation(["netlists", "platforms", "translation"]);

  const available = useMemo(() => {
    const taken = new Set(existingTransportIds.map((id) => id.toUpperCase()));
    return candidates.filter((c) => !taken.has(c.transportId.toUpperCase()));
  }, [candidates, existingTransportIds]);

  const columns = useMemo<ChooserColumnDef<PlatformCandidate>[]>(
    () => [
      { data: "platform", header: t("platforms:header.Platform") },
      { data: "transportId", header: t("netlists:items.transportId") },
      { data: "platformName", header: t("netlists:items.platformName") },
      { data: "agency", header: t("platforms:header.Agency"), defaultContent: "" },
      { data: "config", header: t("platforms:header.Config"), defaultContent: "" },
      {
        data: "description",
        header: t("platforms:header.Description"),
        defaultContent: "",
      },
    ],
    [t],
  );

  const handleAdd = useCallback(
    (ids: number[]) => {
      const picked = new Set(ids);
      onAdd(available.filter((c) => picked.has(c.platformId)));
    },
    [available, onAdd],
  );

  return (
    <MultiSelectorModal<PlatformCandidate, number>
      show={show}
      onHide={onHide}
      onAdd={handleAdd}
      title={t("netlists:items.select_platforms_title")}
      noneMessage={
        mediumType
          ? t("netlists:items.select_platforms_none", { type: mediumType })
          : t("netlists:items.select_platforms_no_type")
      }
      confirmLabel={(count) => t("netlists:items.add_selected", { count })}
      data={available}
      loading={loading}
      getId={(c) => c.platformId}
      columns={columns}
      selectAllAriaLabel={t("netlists:items.select_all_platforms")}
      rowSelectAriaLabel={(c) =>
        t("netlists:items.select_platform", { name: c.platform || c.transportId })
      }
    />
  );
};

export default NetlistPlatformsModal;
