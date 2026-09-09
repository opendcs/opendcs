import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { ApiTimeSeriesIdentifier } from "opendcs-api";
import {
  MultiSelectorModal,
  type ChooserColumnDef,
} from "../../../components/data-table";
import { tsIdColumns } from "./tsIdColumns";

export interface TsGroupMembersAddModalProps {
  show: boolean;
  onHide: () => void;
  /** Every time series in the database; already-added ones are filtered out. */
  catalog: ApiTimeSeriesIdentifier[];
  loading?: boolean;
  excludeKeys: number[];
  onAdd: (keys: number[]) => void;
}

/**
 * The desktop editor's "Add" dialog for explicit members: the full time series
 * catalog, multi-select.
 */
export const TsGroupMembersAddModal: React.FC<TsGroupMembersAddModalProps> = ({
  show,
  onHide,
  catalog,
  loading = false,
  excludeKeys,
  onAdd,
}) => {
  const [t] = useTranslation(["tsgroups", "translation"]);

  const columns = useMemo<ChooserColumnDef<ApiTimeSeriesIdentifier>[]>(
    () => tsIdColumns(t),
    [t],
  );

  return (
    <MultiSelectorModal<ApiTimeSeriesIdentifier, number>
      show={show}
      onHide={onHide}
      onAdd={onAdd}
      title={t("tsgroups:members.add")}
      noneMessage={t("tsgroups:members.add_none")}
      confirmLabel={(count) => t("tsgroups:members.add_selected", { count })}
      data={catalog}
      loading={loading}
      getId={(ts) => ts.key!}
      columns={columns}
      excludeIds={excludeKeys}
      selectAllAriaLabel={t("tsgroups:members.select_all")}
      rowSelectAriaLabel={(ts) =>
        t("tsgroups:members.select", { name: ts.uniqueString ?? ts.key })
      }
    />
  );
};

export default TsGroupMembersAddModal;
