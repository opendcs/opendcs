import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import type { ApiTsGroupRef } from "opendcs-api";
import {
  AppDataTable,
  type ColumnDef,
  type HeaderButton,
  type RowAction,
} from "../../../components/data-table";
import { SUBGROUP_COMBINES, type SubGroupCombine } from "./TsGroupReducer";
import { SubGroupAddModal } from "./SubGroupAddModal";

export interface SubGroupsTableProperties {
  /** The group's sub-groups, keyed by how each is combined into it. */
  subGroups: Record<SubGroupCombine, ApiTsGroupRef[]>;
  /** All groups in the database, backing the chooser. */
  allGroups: ApiTsGroupRef[];
  allGroupsLoading?: boolean;
  /** This group's own id — a group cannot be its own sub-group. */
  selfId?: number;
  edit?: boolean;
  onAdd: (combine: SubGroupCombine, groups: ApiTsGroupRef[]) => void;
  onRemove: (combine: SubGroupCombine, groupId: number) => void;
}

interface SubGroupRow {
  key: string;
  combine: SubGroupCombine;
  group: ApiTsGroupRef;
}

/**
 * The desktop editor's "Sub-Group Members" table. The API keeps the three
 * combine modes in separate lists; they are shown here as one table with a
 * "Combine" column, matching the desktop layout.
 */
export const SubGroupsTable: React.FC<SubGroupsTableProperties> = ({
  subGroups,
  allGroups,
  allGroupsLoading = false,
  selfId,
  edit = false,
  onAdd,
  onRemove,
}) => {
  const [t] = useTranslation(["tsgroups", "translation"]);
  const [addCombine, setAddCombine] = useState<SubGroupCombine | undefined>();

  const rows = useMemo<SubGroupRow[]>(
    () =>
      SUBGROUP_COMBINES.flatMap((combine) =>
        subGroups[combine].map((group) => ({
          key: `${combine}:${group.groupId}`,
          combine,
          group,
        })),
      ),
    [subGroups],
  );

  const columns = useMemo<ColumnDef<SubGroupRow>[]>(
    () => [
      {
        data: null,
        header: t("tsgroups:header.Id"),
        defaultContent: "",
        type: "num",
        render: (_d, _type, row) => row.group.groupId ?? "",
      },
      {
        data: null,
        header: t("tsgroups:header.Name"),
        defaultContent: "",
        render: (_d, _type, row) => row.group.groupName ?? "",
      },
      {
        data: null,
        header: t("tsgroups:header.Type"),
        defaultContent: "",
        render: (_d, _type, row) => row.group.groupType ?? "",
      },
      {
        data: null,
        header: t("tsgroups:header.Description"),
        defaultContent: "",
        render: (_d, _type, row) => row.group.description ?? "",
      },
      {
        data: null,
        header: t("tsgroups:subgroups.combine"),
        defaultContent: "",
        render: (_d, _type, row) => t(`tsgroups:subgroups.combine_${row.combine}`),
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<SubGroupRow>[]>(() => {
    if (!edit) return [];
    return [
      {
        key: "remove",
        icon: "bi-trash",
        variant: "danger",
        confirm: true,
        aria: (row) => t("tsgroups:subgroups.remove", { name: row.group.groupName }),
        onClick: ({ row }) => {
          if (row.group.groupId !== undefined) {
            onRemove(row.combine, row.group.groupId);
          }
        },
      },
    ];
  }, [edit, t, onRemove]);

  const extraHeaderButtons = useMemo<HeaderButton[]>(() => {
    if (!edit) return [];
    return SUBGROUP_COMBINES.map((combine) => ({
      text: t(`tsgroups:subgroups.button_${combine}`),
      ariaLabel: t(`tsgroups:subgroups.add_${combine}`),
      onClick: () => setAddCombine(combine),
    }));
  }, [edit, t]);

  // A group may not include itself, nor the same sub-group twice under the
  // same combine mode. It may legitimately appear under two different modes,
  // so only the mode being added to is excluded.
  const excludeIds = useMemo(() => {
    const taken = addCombine ? subGroups[addCombine].map((g) => g.groupId ?? -1) : [];
    return selfId !== undefined ? [...taken, selfId] : taken;
  }, [addCombine, subGroups, selfId]);

  return (
    <>
      <AppDataTable<SubGroupRow, string>
        data={rows}
        getId={(r) => r.key}
        columns={columns}
        actionsLabel={edit ? t("translation:actions") : undefined}
        rowActions={rowActions}
        extraHeaderButtons={extraHeaderButtons}
        caption={t("tsgroups:subgroups.title")}
        tableId="tsGroupSubGroupsTable"
      />
      <SubGroupAddModal
        combine={addCombine}
        onHide={() => setAddCombine(undefined)}
        groups={allGroups}
        loading={allGroupsLoading}
        excludeIds={excludeIds}
        onAdd={(ids) => {
          if (!addCombine) return;
          const byId = new Map(allGroups.map((g) => [g.groupId, g]));
          onAdd(
            addCombine,
            ids
              .map((id) => byId.get(id))
              .filter((g): g is ApiTsGroupRef => g !== undefined),
          );
        }}
      />
    </>
  );
};

export default SubGroupsTable;
