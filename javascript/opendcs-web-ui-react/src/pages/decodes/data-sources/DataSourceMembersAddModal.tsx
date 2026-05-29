import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiDataSourceRef } from "opendcs-api";
import {
  MultiSelectorModal,
  type ChooserColumnDef,
} from "../../../components/data-table";

export interface DataSourceMembersAddModalProps {
  show: boolean;
  onHide: () => void;
  /** All available data sources (the modal filters out already-selected + self). */
  dataSources: ApiDataSourceRef[];
  loading?: boolean;
  /** Names already attached as group members, plus the group's own name. */
  excludeNames: string[];
  onAdd: (names: string[]) => void;
}

export const DataSourceMembersAddModal: React.FC<DataSourceMembersAddModalProps> = ({
  show,
  onHide,
  dataSources,
  loading = false,
  excludeNames,
  onAdd,
}) => {
  const [t] = useTranslation(["datasources", "translation"]);

  const columns = useMemo<ChooserColumnDef<ApiDataSourceRef>[]>(
    () => [
      { data: "dataSourceId", header: t("datasources:header.Id"), type: "num" },
      { data: "name", header: t("datasources:header.Name") },
      { data: "type", header: t("datasources:header.Type"), defaultContent: "" },
      {
        data: "arguments",
        header: t("datasources:header.Arguments"),
        defaultContent: "",
      },
    ],
    [t],
  );

  return (
    <MultiSelectorModal<ApiDataSourceRef, string>
      show={show}
      onHide={onHide}
      onAdd={onAdd}
      title={t("datasources:add_members")}
      noneMessage={t("datasources:add_members_none")}
      confirmLabel={(count) => t("datasources:add_selected", { count })}
      data={dataSources}
      loading={loading}
      getId={(d) => d.name ?? ""}
      columns={columns}
      excludeIds={excludeNames}
      selectAllAriaLabel={t("datasources:select_all_members")}
      rowSelectAriaLabel={(d) =>
        t("datasources:select_member", { name: d.name ?? d.dataSourceId })
      }
    />
  );
};

export default DataSourceMembersAddModal;
