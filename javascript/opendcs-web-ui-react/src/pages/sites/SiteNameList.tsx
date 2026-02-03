import DataTable, {
  type DataTableProps,
  type DataTableRef,
  type DataTableSlots,
} from "datatables.net-react";
import DT, { type CellSelector } from "datatables.net-bs5";
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
  siteNames: Partial<SiteNameType>[];
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

  useEffect(() => {
    rowStateRef.current = rowState;
  }, [rowState]);

  console.log(`Is editable = ${edit}`);
  const renderEditableType = useCallback(
    (
      data: string | number | readonly string[] | undefined,
      type: string,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _row: any,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _meta: any,
    ) => {
      if (type !== "display") {
        return data;
      }

      if (data === undefined) {
        //
        try {
          const container = toDom(
            <SiteNameTypeSelect
              defaultValue={refList(REFLIST_SITE_NAME_TYPE).defaultValue}
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
    [rowStateRef],
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
      console.log("render name: " + data);
      if (type !== "display") {
        return data;
      }
      console.log(`row -> ${JSON.stringify(row)}`);
      console.log(`rowState ${JSON.stringify(rowStateRef.current)}`);
      if (data === undefined || rowStateRef.current[row.type!] === "edit") {
        try {
          const container = toDom(
            <input
              type="text"
              name="value"
              defaultValue={data}
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
    [rowStateRef],
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
          row.cell({ column: 1 } as CellSelector).draw(false);
        }
      });
    }
  }, [rowState, siteNames, edit, rowStateRef]);

  const slots = useMemo<DataTableSlots>(() => {
    return {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      actions: (data: Partial<SiteNameType>, _row: SiteNameType) => {
        console.log("calling action slot");
        const inEdit =
          data.type === undefined || rowStateRef.current[data.type!] === "edit";
        return (
          <>
            {edit && inEdit && (
              <Button
                onClick={() => {
                  const snt: SiteNameType = data as SiteNameType;
                  actions.save!(snt);
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
                    console.log(`Updating ${data.type} to edit`);
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
            {edit && actions.remove !== undefined && (
              <Button
                onClick={() => {
                  actions.remove!(data as SiteNameType);
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
    };
  }, [edit, rowStateRef, i18n.language]);

  const options: DataTableProps["options"] = {
    paging: false,
    responsive: false,
    search: false,
    info: false,
    searching: false,
    language: dtLangs.get(i18n.language),
    layout: {
      top1Start: {
        buttons:
          actions?.add !== undefined
            ? [
                {
                  text: "+",
                  action: () => {
                    actions.add?.({ name: "new", type: "new" });
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
      data={siteNames}
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
