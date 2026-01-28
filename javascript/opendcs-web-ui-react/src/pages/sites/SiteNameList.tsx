import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5";
import { Suspense, useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import { dtLangs } from "../../lang";
import type { CollectionActions } from "../../util/Actions";
import SiteNameTypeSelect from "./SiteNameTypeSelect";
import { createRoot } from "react-dom/client";
import RefListContext, { useRefList } from "../../contexts/data/RefListContext";
import { useContextWrapper } from "../../util/ContextWrapper";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(dtButtons);

export type SiteNameType = { type: string; name: string };

interface SiteNameListProperties {
  siteNames: { [k: string]: string };
  actions?: CollectionActions<SiteNameType>;
}

export const SiteNameList: React.FC<SiteNameListProperties> = ({
  siteNames,
  actions,
}) => {
  const { toDom } = useContextWrapper();
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["sites"]);

  const renderEditableType = (
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

    if (actions?.edit !== undefined) {
      try {
        const container = toDom(<SiteNameTypeSelect current={row.type} />);
        return container;
      } catch (error) {
        return JSON.stringify(error);
      }
    } else {
      return data;
    }
  };

  const site_names = useMemo(() => {
    return Object.entries(siteNames).map(([k, v]) => {
      return { type: k, value: v };
    });
  }, [siteNames]);

  const columns = [
    { data: "type", render: renderEditableType },
    { data: "value" },
    {
      data: null,
      name: "actions",
      render: () => {
        return "action";
      },
    },
  ];

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
                    console.log("Add site name");
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
      data={site_names}
      options={options}
      ref={table}
      className="table table-hover table-striped tablerow-cursor w-100 border"
    >
      <thead>
        <tr>
          <th>{t("sites:site_names.name_type")}</th>
          <th>{t("translation:value")}</th>
          <th>{t("translation:actions")}</th>
        </tr>
      </thead>
    </DataTable>
  );
};

export default SiteNameList;
