import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import type { ApiTimeSeriesIdentifier } from "opendcs-api";
import {
  AppDataTable,
  type ColumnDef,
  type HeaderButton,
  type RowAction,
} from "../../../components/data-table";
import { tsIdColumns } from "./tsIdColumns";
import { TsGroupMembersAddModal } from "./TsGroupMembersAddModal";

export interface TsGroupMembersTableProperties {
  /** Time series explicitly named as members of this group. */
  members: ApiTimeSeriesIdentifier[];
  /** Full time series catalog backing the chooser. */
  catalog: ApiTimeSeriesIdentifier[];
  catalogLoading?: boolean;
  edit?: boolean;
  onAdd: (tsIds: ApiTimeSeriesIdentifier[]) => void;
  onRemove: (key: number) => void;
}

/**
 * The desktop editor's "Time Series Group Members" table: time series named
 * explicitly, as opposed to the ones a sub-group or a criterion pulls in.
 */
export const TsGroupMembersTable: React.FC<TsGroupMembersTableProperties> = ({
  members,
  catalog,
  catalogLoading = false,
  edit = false,
  onAdd,
  onRemove,
}) => {
  const [t] = useTranslation(["tsgroups", "translation"]);
  const [showAddModal, setShowAddModal] = useState(false);

  const columns = useMemo<ColumnDef<ApiTimeSeriesIdentifier>[]>(
    () => tsIdColumns(t),
    [t],
  );

  const rowActions = useMemo<RowAction<ApiTimeSeriesIdentifier>[]>(() => {
    if (!edit) return [];
    return [
      {
        key: "remove",
        icon: "bi-trash",
        variant: "danger",
        confirm: true,
        aria: (row) => t("tsgroups:members.remove", { name: row.uniqueString }),
        onClick: ({ row }) => {
          if (row.key !== undefined) onRemove(row.key);
        },
      },
    ];
  }, [edit, t, onRemove]);

  const extraHeaderButtons = useMemo<HeaderButton[]>(() => {
    if (!edit) return [];
    return [
      {
        text: "+",
        ariaLabel: t("tsgroups:members.add"),
        onClick: () => setShowAddModal(true),
      },
    ];
  }, [edit, t]);

  const selectedKeys = useMemo(
    () => members.map((m) => m.key).filter((k): k is number => k !== undefined),
    [members],
  );

  return (
    <>
      <AppDataTable<ApiTimeSeriesIdentifier, number>
        data={members}
        getId={(ts) => ts.key!}
        columns={columns}
        actionsLabel={edit ? t("translation:actions") : undefined}
        rowActions={rowActions}
        extraHeaderButtons={extraHeaderButtons}
        caption={t("tsgroups:members.title")}
        tableId="tsGroupMembersTable"
      />
      <TsGroupMembersAddModal
        show={showAddModal}
        onHide={() => setShowAddModal(false)}
        catalog={catalog}
        loading={catalogLoading}
        excludeKeys={selectedKeys}
        onAdd={(keys) => {
          const byKey = new Map(catalog.map((ts) => [ts.key, ts]));
          onAdd(
            keys
              .map((k) => byKey.get(k))
              .filter((ts): ts is ApiTimeSeriesIdentifier => ts !== undefined),
          );
        }}
      />
    </>
  );
};

export default TsGroupMembersTable;
