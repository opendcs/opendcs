import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import type { ApiDataSourceGroupMember, ApiDataSourceRef } from "opendcs-api";
import {
  AppDataTable,
  type ColumnDef,
  type HeaderButton,
  type RowAction,
} from "../../../components/data-table";
import { useDataSourceRefsQuery } from "../../../queries/dataSources";
import { DataSourceMembersAddModal } from "./DataSourceMembersAddModal";

export interface DataSourceMembersTableProperties {
  /** Members currently attached to this group data source. */
  members: ApiDataSourceGroupMember[];
  /** This data source's own name — excluded from the chooser to avoid self-reference. */
  selfName?: string;
  edit?: boolean;
  onAdd: (members: ApiDataSourceGroupMember[]) => void;
  onRemove: (name: string) => void;
}

// Stable fallback so the memoised rows don't rebuild on every render while loading.
const NO_DATA_SOURCES: ApiDataSourceRef[] = [];

interface MemberRow {
  key: string;
  name: string;
  ref?: ApiDataSourceRef;
}

/**
 * Queries the data source list itself rather than taking it as a prop: this
 * renders inside an AppDataTable detail row, which is built once per open and
 * never re-rendered with new props, so a list passed down while still loading
 * would leave the member chooser empty for as long as the row is open (#2202).
 */
export const DataSourceMembersTable: React.FC<DataSourceMembersTableProperties> = ({
  members,
  selfName,
  edit = false,
  onAdd,
  onRemove,
}) => {
  const [t] = useTranslation(["datasources", "translation"]);
  const [showAddModal, setShowAddModal] = useState(false);
  const { data: allDataSources = NO_DATA_SOURCES, isFetching } =
    useDataSourceRefsQuery();

  const refsByName = useMemo(() => {
    const m = new Map<string, ApiDataSourceRef>();
    for (const d of allDataSources) {
      if (d.name !== undefined) m.set(d.name, d);
    }
    return m;
  }, [allDataSources]);

  const rows = useMemo<MemberRow[]>(
    () =>
      members.map((member) => ({
        key: member.dataSourceName ?? String(member.dataSourceId ?? ""),
        name: member.dataSourceName ?? "",
        ref: member.dataSourceName ? refsByName.get(member.dataSourceName) : undefined,
      })),
    [members, refsByName],
  );

  const selectedNames = useMemo(
    () => members.map((m) => m.dataSourceName ?? "").filter(Boolean),
    [members],
  );

  const excludeNames = useMemo(
    () => (selfName ? [...selectedNames, selfName] : selectedNames),
    [selectedNames, selfName],
  );

  const columns = useMemo<ColumnDef<MemberRow>[]>(
    () => [
      {
        data: null,
        header: t("datasources:header.Id"),
        defaultContent: "",
        type: "num",
        render: (_d, _t, row) => row.ref?.dataSourceId ?? "",
      },
      {
        data: null,
        header: t("datasources:header.Name"),
        defaultContent: "",
        render: (_d, _t, row) => row.ref?.name ?? row.name,
      },
      {
        data: null,
        header: t("datasources:header.Type"),
        defaultContent: "",
        render: (_d, _t, row) => row.ref?.type ?? "",
      },
    ],
    [t],
  );

  const rowActions = useMemo<RowAction<MemberRow>[]>(() => {
    if (!edit) return [];
    return [
      {
        key: "remove",
        icon: "bi-trash",
        variant: "danger",
        confirm: true,
        aria: (row) => t("datasources:remove_member", { name: row.name }),
        onClick: ({ row }) => onRemove(row.name),
      },
    ];
  }, [edit, t, onRemove]);

  const extraHeaderButtons = useMemo<HeaderButton[]>(() => {
    if (!edit) return [];
    return [
      {
        text: t("datasources:add_members"),
        ariaLabel: t("datasources:add_members"),
        icon: "bi-plus-lg",
        onClick: () => setShowAddModal(true),
      },
    ];
  }, [edit, t]);

  const handleAdd = (names: string[]) => {
    onAdd(
      names.map((name) => ({
        dataSourceId: refsByName.get(name)?.dataSourceId,
        dataSourceName: name,
      })),
    );
  };

  return (
    <>
      <AppDataTable<MemberRow, string>
        data={rows}
        getId={(r) => r.key}
        columns={columns}
        actionsLabel={edit ? t("translation:actions") : undefined}
        rowActions={rowActions}
        extraHeaderButtons={extraHeaderButtons}
        caption={t("datasources:group_members")}
        tableId="dataSourceMembersTable"
      />
      <DataSourceMembersAddModal
        show={showAddModal}
        onHide={() => setShowAddModal(false)}
        dataSources={allDataSources}
        loading={isFetching}
        excludeNames={excludeNames}
        onAdd={handleAdd}
      />
    </>
  );
};

export default DataSourceMembersTable;
