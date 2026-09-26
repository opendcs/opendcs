import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AppDataTable,
  type ColumnDef,
  type HeaderButton,
  type RowAction,
} from "../../../components/data-table";
import type { Criterion } from "./groupCriteria";
import type { NewCriterion } from "./TsGroupReducer";
import { CRITERION_PARTS, CriterionModal, type CriterionPart } from "./CriterionModal";

export interface GroupCriteriaTableProperties {
  criteria: Criterion[];
  edit?: boolean;
  onAdd: (criterion: NewCriterion) => void;
  onRemove: (key: string) => void;
}

/**
 * The desktop editor's "Select by site, data-type, interval, or
 * statistics-code" panel: one TSID Part / Value row per criterion, with a
 * button per part to add another.
 *
 * Values for the same part are OR'ed together and different parts are AND'ed,
 * which is the toolkit's own group-expansion rule — see the Time Series Group
 * Editor chapter of the computation processor user guide.
 */
export const GroupCriteriaTable: React.FC<GroupCriteriaTableProperties> = ({
  criteria,
  edit = false,
  onAdd,
  onRemove,
}) => {
  const [t] = useTranslation(["tsgroups", "translation"]);
  const [addPart, setAddPart] = useState<CriterionPart | undefined>();

  const columns = useMemo<ColumnDef<Criterion>[]>(
    () => [
      {
        data: "part",
        header: t("tsgroups:criteria.header.TsidPart"),
        render: (_d, type, row) =>
          type === "display"
            ? t(`tsgroups:criteria.part.${row.part}`, row.part)
            : row.part,
      },
      {
        data: "value",
        header: t("tsgroups:criteria.header.Value"),
        defaultContent: "",
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<Criterion>[]>(() => {
    if (!edit) return [];
    return [
      {
        key: "remove",
        icon: "bi-trash",
        variant: "danger",
        confirm: true,
        aria: (row) =>
          t("tsgroups:criteria.remove", { part: row.part, value: row.value }),
        onClick: ({ row }) => onRemove(row.key),
      },
    ];
  }, [edit, t, onRemove]);

  const extraHeaderButtons = useMemo<HeaderButton[]>(() => {
    if (!edit) return [];
    return CRITERION_PARTS.map((part) => ({
      text: t(`tsgroups:criteria.part.${part}`),
      ariaLabel: t("tsgroups:criteria.add_title", {
        part: t(`tsgroups:criteria.part.${part}`),
      }),
      onClick: () => setAddPart(part),
    }));
  }, [edit, t]);

  return (
    <>
      <AppDataTable<Criterion, string>
        data={criteria}
        getId={(c) => c.key}
        columns={columns}
        actionsLabel={edit ? t("translation:actions") : undefined}
        rowActions={rowActions}
        extraHeaderButtons={extraHeaderButtons}
        caption={t("tsgroups:criteria.title")}
        tableId="tsGroupCriteriaTable"
      />
      <CriterionModal
        part={addPart}
        onHide={() => setAddPart(undefined)}
        onAdd={onAdd}
      />
    </>
  );
};

export default GroupCriteriaTable;
