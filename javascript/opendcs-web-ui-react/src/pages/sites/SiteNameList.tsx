import DataTable, { type DataTableProps, type DataTableRef } from "datatables.net-react";
import DT from "datatables.net-bs5";
import { useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import { dtLangs } from "../../lang";
import type { Actions } from "../../util/Actions";

// eslint-disable-next-line react-hooks/rules-of-hooks

DataTable.use(DT);

interface SiteNameListProperties {
    siteNames: {[k: string]: string}
    actions?: Actions<String>;
}

export const SiteNameList: React.FC<SiteNameListProperties> = ({siteNames, actions}) => {
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["sites"]);

  const site_names = useMemo(() => {
    return Object.entries(siteNames).map(([k,v]) => {
         return {type: k, value: v};
    })}, [siteNames]);
  console.log(JSON.stringify(site_names));
  const columns = [
    { data: "type" },
    { data: "value" },
    { data: null, name: "actions", render: () => {return "action";} },
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
        buttons: actions?.add !== undefined ? [
           {
            text: "+",
            action: () => {
              console.log("Add site name")
            },
            attr: {
              "aria-label": t("sites:add_site_name"),
            },
          },
        ] : [],
      },
    }
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
								<th>{t("sites:site_name_type")}</th>
								<th>{t("translation:value")}</th>
                                <th>{t("translation:actions")}</th>
							</tr>
						</thead>
        </DataTable>
  );
}

export default SiteNameList;