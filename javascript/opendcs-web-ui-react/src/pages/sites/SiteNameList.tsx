import DataTable, {
  type DataTableProps,
  type DataTableRef,
  type DataTableSlots,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5";
import { useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import { dtLangs } from "../../lang";
import type { CollectionActions, UiState } from "../../util/Actions";
import SiteNameTypeSelect from "./SiteNameTypeSelect";
import { useContextWrapper } from "../../util/ContextWrapper";
import { Button } from "react-bootstrap";
import { Pencil, Save, Trash } from "react-bootstrap-icons";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(dtButtons);

export type SiteNameType = { type: string; name: string; ui_state?: UiState };

interface SiteNameListProperties {
  siteNames: Partial<SiteNameType>[];
  actions?: CollectionActions<SiteNameType>;
}

export const SiteNameList: React.FC<SiteNameListProperties> = ({
  siteNames,
  actions = {},
}) => {
  const { toDom } = useContextWrapper();
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["sites"]);

  const renderEditableType = (
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

    if (actions?.edit !== undefined || data === undefined) {
      try {
        const container = toDom(<SiteNameTypeSelect current={data as string} />);
        return container;
      } catch (error) {
        return JSON.stringify(error);
      }
    } else {
      return data;
    }
  };

  const renderEditableName = (
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

    if (data === undefined || row.ui_state === "edit") {
      try {
        const container = toDom(<input type="text" name="value" defaultValue={data} />);
        return container;
      } catch (error) {
        return JSON.stringify(error);
      }
    } else {
      return data;
    }
  };

  const columnsBase = [
    { data: "type", render: renderEditableType, defaultContent: "" },
    { data: "name", render: renderEditableName, defaultContent: "" },
  ];

  const actionColumn = {
    data: null,
    name: "actions",
  };
  const columns = useMemo(
    () => (actions?.edit === undefined ? columnsBase : [...columnsBase, actionColumn]),
    [actions],
  );

  const slots = useMemo<DataTableSlots>(() => {
    return {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      actions: (data: Partial<SiteNameType>, type: unknown, _row: SiteNameType) => {
        if (type === "display") {
          const inEdit = data.ui_state === "new" || data.ui_state === "edit";
          console.log(data);
          return (
            <>
              {inEdit && (
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
              {actions.edit !== undefined && !inEdit && (
                <Button
                  onClick={() => {
                    actions.edit!(data as SiteNameType);
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
              {actions.remove !== undefined && (
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
        } else {
          return data;
        }
      },
    };
  }, [columns]);

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
