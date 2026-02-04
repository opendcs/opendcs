import DataTable, {
  type DataTableProps,
  type DataTableRef,
  type DataTableSlots,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { dtLangs } from "../../lang";
import type { CollectionActions } from "../../util/Actions";
import SiteNameTypeSelect from "./SiteNameTypeSelect";
import { useContextWrapper } from "../../util/ContextWrapper";
import { Button } from "react-bootstrap";
import { Pencil, Save, Trash } from "react-bootstrap-icons";
import { REFLIST_SITE_NAME_TYPE, useRefList } from "../../contexts/data/RefListContext";
import type { RowState } from "../../util/DataTables";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(dtButtons);

export type SiteNameType = { type: string; name: string };

interface SiteNameListProperties {
  siteNames: SiteNameType[];
  actions?: CollectionActions<SiteNameType>;
  edit?: boolean;
}

export const SiteNameList: React.FC<SiteNameListProperties> = ({
  siteNames,
  actions = {},
  edit = false,
}) => {
  const { toDom } = useContextWrapper();
  const { refList } = useRefList();
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["sites"]);
  const [rowState, updateRowState] = useState<RowState<string>>({});
  const rowStateRef = useRef(rowState);
  const rowInputRef = useRef<Record<string, { type?: string; name?: string }>>({});
  const [localSiteNames, updateLocalSitenames] = useState<Partial<SiteNameType>[]>([]);
  const localSiteNamesRef = useRef(localSiteNames);
  const siteNamesRef = useRef(siteNames);

  useEffect(() => {
    siteNamesRef.current = siteNames;
  }, [siteNames]);

  const allSites = useMemo(
    () => [...siteNames, ...localSiteNames],
    [siteNames, localSiteNames],
  );

  useEffect(() => {
    rowStateRef.current = rowState;
  }, [rowState]);

  useEffect(() => {
    localSiteNamesRef.current = localSiteNames;
  }, [localSiteNames]);

  const renderEditableType = useCallback(
    (
      data: string | number | readonly string[] | undefined,
      type: string,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      row: any,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _meta: any,
    ) => {
      if (type !== "display") {
        return data;
      }

      if (data === "-") {
        //
        try {
          rowInputRef.current[row.type] = {
            ...rowInputRef.current[row.type],
            type: refList(REFLIST_SITE_NAME_TYPE).defaultValue,
          };
          const container = toDom(
            <SiteNameTypeSelect
              onChange={(e) => {
                e.stopPropagation();
                rowInputRef.current[row.type] = {
                  ...rowInputRef.current[row.type],
                  type: e.target.value,
                };
              }}
              defaultValue={refList(REFLIST_SITE_NAME_TYPE).defaultValue}
              existing={siteNamesRef.current}
            />,
          );
          return container;
        } catch (error) {
          return JSON.stringify(error);
        }
      } else {
        return data;
      }
    },
    [rowStateRef, rowInputRef, siteNamesRef],
  );

  const renderEditableName = useCallback(
    (
      data: string | number | readonly string[] | undefined,
      type: string,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      row: any,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _meta: any,
    ) => {
      if (type !== "display") {
        return data;
      }
      if (data === undefined || rowStateRef.current[row.type!] === "edit") {
        try {
          const container = toDom(
            <input
              type="text"
              name="value"
              defaultValue={data}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                e.stopPropagation();
                rowInputRef.current[row.type] = {
                  ...rowInputRef.current[row.type],
                  name: e.target.value,
                };
              }}
              aria-label={t("sites:site_names.value_input_for", { type: row.type })}
            />,
          );
          return container;
        } catch (error) {
          return JSON.stringify(error);
        }
      } else {
        return data;
      }
    },
    [rowState, rowStateRef, rowInputRef],
  );

  const columnsBase = useMemo(
    () => [
      { data: "type", render: renderEditableType, defaultContent: "" },
      { data: "name", render: renderEditableName, defaultContent: "" },
    ],
    [rowStateRef],
  );

  const actionColumn = {
    data: null,
    name: "actions",
  };
  const columns = useMemo(
    () => (edit ? [...columnsBase, actionColumn] : columnsBase),
    [edit, columnsBase],
  );

  useEffect(() => {
    if (table.current?.dt()) {
      const dt = table.current.dt()!;
      const visibleRows = dt.rows({ page: "current", search: "applied" });
      visibleRows.every(function () {
        const row = this;
        const idx = (row.data() as SiteNameType).type;
        if (rowState[idx] !== undefined) {
          row.invalidate().draw(false);
        }
      });
    }
  }, [rowState, siteNames, edit, rowStateRef]);

  const renderActions = useCallback(
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    (data: Partial<SiteNameType>, _row: SiteNameType) => {
      const inEdit = data.type === "-" || rowStateRef.current[data.type!] === "edit";
      return (
        <>
          {edit && inEdit && (
            <Button
              onClick={() => {
                const snt: SiteNameType = data as SiteNameType;
                actions.save!({
                  type:
                    snt.type.charAt(0) !== "-"
                      ? snt.type
                      : rowInputRef.current[snt.type].type!,
                  name: rowInputRef.current[snt.type].name!,
                });
                delete rowInputRef.current[snt.type];
                updateLocalSitenames((prev) => {
                  return [...prev.filter((sn) => sn.type !== data.type)];
                });
                updateRowState((prev) => {
                  return {
                    ...prev,
                    [snt.type]: "show",
                  };
                });
              }}
              variant="primary"
              size="sm"
              aria-label={t("sites:site_names.save_for", {
                type: data.type,
                name: data.name,
              })}
            >
              <Save />
            </Button>
          )}
          {edit && !inEdit && (
            <Button
              onClick={() => {
                updateRowState((prev) => {
                  return {
                    ...prev,
                    [data.type!]: "edit",
                  };
                });
              }}
              variant="warning"
              size="sm"
              aria-label={t("sites:site_names.edit_for", {
                type: data.type,
                name: data.name,
              })}
            >
              <Pencil />
            </Button>
          )}
          {edit && (actions.remove !== undefined || data.type === "-") && (
            <Button
              onClick={() => {
                const snt = data as SiteNameType;
                if (localSiteNamesRef.current?.find((sn) => sn.type === snt.type)) {
                  updateLocalSitenames((prev) => [
                    ...prev.filter((sn) => sn.type !== snt.type),
                  ]);
                } else {
                  actions.remove!(data as SiteNameType);
                }
              }}
              variant="danger"
              size="sm"
              aria-label={t("sites:site_names.delete_for", {
                type: data.type,
                name: data.name,
              })}
            >
              <Trash />
            </Button>
          )}
        </>
      );
    },
    [localSiteNamesRef, edit, i18n.language, rowStateRef],
  );

  const slots = {
    actions: renderActions,
  };

  const options: DataTableProps["options"] = {
    paging: false,
    responsive: false,
    search: false,
    info: false,
    searching: false,
    language: dtLangs.get(i18n.language),
    layout: {
      top1Start: {
        buttons: edit
          ? [
              {
                text: "+",
                action: () => {
                  updateLocalSitenames((prev) => {
                    return [...prev, { type: "-" }];
                  });
                },
                attr: {
                  "aria-label": t("sites:site_names.add_name"),
                },
              },
            ]
          : [],
      },
    },
  };

  return (
    <DataTable
      key={i18n.language} // convient way to force a render of the components and aria labels on a language change.
      id="siteNamesTable"
      columns={columns}
      data={allSites}
      options={options}
      slots={columns.length === 3 ? slots : undefined}
      ref={table}
      className="table table-hover table-striped tablerow-cursor w-100 border"
    >
      <thead>
        <tr>
          <th>{t("sites:site_names.name_type")}</th>
          <th>{t("translation:value")}</th>
          {columns.length === 3 ? <th>{t("translation:actions")}</th> : null}
        </tr>
      </thead>
    </DataTable>
  );
};

export default SiteNameList;
