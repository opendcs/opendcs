import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiTsGroupRef } from "opendcs-api";
import {
  MultiSelectorModal,
  type ChooserColumnDef,
} from "../../../components/data-table";
import type { SubGroupCombine } from "./TsGroupReducer";

export interface SubGroupAddModalProps {
  /** Which combine button opened the dialog, or undefined when it is closed. */
  combine?: SubGroupCombine;
  onHide: () => void;
  /** All groups in the database; this group and its existing subgroups are excluded. */
  groups: ApiTsGroupRef[];
  loading?: boolean;
  excludeIds: number[];
  onAdd: (groupIds: number[]) => void;
}

/**
 * Chooser behind "Add / Subtract / Intersect SubGroup". The three buttons all
 * open this one dialog; `combine` decides the title and which list the picked
 * groups land in.
 */
export const SubGroupAddModal: React.FC<SubGroupAddModalProps> = ({
  combine,
  onHide,
  groups,
  loading = false,
  excludeIds,
  onAdd,
}) => {
  const [t] = useTranslation(["tsgroups", "translation"]);

  const columns = useMemo<ChooserColumnDef<ApiTsGroupRef>[]>(
    () => [
      { data: "groupId", header: t("tsgroups:header.Id"), type: "num" },
      { data: "groupName", header: t("tsgroups:header.Name") },
      { data: "groupType", header: t("tsgroups:header.Type"), defaultContent: "" },
      {
        data: "description",
        header: t("tsgroups:header.Description"),
        defaultContent: "",
      },
    ],
    [t],
  );

  return (
    <MultiSelectorModal<ApiTsGroupRef, number>
      show={combine !== undefined}
      onHide={onHide}
      onAdd={onAdd}
      title={t(`tsgroups:subgroups.add_${combine ?? "include"}`)}
      noneMessage={t("tsgroups:subgroups.add_none")}
      confirmLabel={(count) => t("tsgroups:subgroups.add_selected", { count })}
      data={groups}
      loading={loading}
      getId={(g) => g.groupId!}
      columns={columns}
      excludeIds={excludeIds}
      selectAllAriaLabel={t("tsgroups:subgroups.select_all")}
      rowSelectAriaLabel={(g) =>
        t("tsgroups:subgroups.select", { name: g.groupName ?? g.groupId })
      }
    />
  );
};

export default SubGroupAddModal;
