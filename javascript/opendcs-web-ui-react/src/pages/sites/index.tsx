import DataTable, { type DataTableProps, type DataTableRef } from "datatables.net-react";
import DT from "datatables.net-bs5";
import { useTranslation } from "react-i18next";
import { Suspense, useEffect, useRef } from "react";
import { dtLangs } from "../../lang";
import type { ApiSite } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { renderToString } from "react-dom/server";
import Site from "./Site";


// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

interface SiteTableProperties {
  sites: ApiSite[];
};


export const SitesTable: React.FC<SiteTableProperties> = ({sites}) => {
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["sites"]);

  const columns = [
    { data: "siteId" },
    { data: null,
      render: (_data: unknown, _type: unknown, row: unknown) => {
        const site = row as ApiSite;
        return site.sitenames?.CWMS;
      }
    },
    { data: "description", defaultContent: "" },
    { data: null, name: "actions", render: () => {return "action";} },
  ];

  const options: DataTableProps["options"] = {
    paging: true,
    responsive: true,
    language: dtLangs.get(i18n.language),
    layout: {
      top1Start: {
        buttons: [
          {
            text: "+",
            action: () => {
              console.log("Add site")
            },
            attr: {
              "aria-label": t("sites:add_site"),
            },
          },
        ],
      },
    }
  };

  useEffect(() => {
    // Add event listener for opening and closing details
    table.current?.dt()!.on('click', 'tbody td', function (e) {
        const dt = table.current!.dt()!;
        const tr = (e.target! as Element).closest('tr');
        const row = dt.row(tr as HTMLTableRowElement);
        
        if (row.child.isShown()) {
            // This row is already open - close it
            row.child.hide();
        }
        else {
            const data: ApiSite = row.data as ApiSite;
            // Open this row
            row.child(renderToString(<Suspense fallback="Loading..."><Site site={data}/></Suspense>)).show();
        }
    });
  }, [i18n.language]);

  return (
        <DataTable
          key={i18n.language} // convient way to force a render of the components and aria labels on a language change.
          id="siteTable"
          columns={columns}
          data={sites}
          options={options}
          ref={table}
          className="table table-hover table-striped tablerow-cursor w-100 border"
        >
          <caption className="captionTitleCenter">
          {t("sites:title")}
        </caption>
          <thead>
							<tr>
								<th>{t("sites:site_id")}</th>
								<th id="siteNameColumnHeader">{t("sites:site_name")}</th>
								<th>{t("sites:description")}</th>
                <th>{t("translation:actions")}</th>
							</tr>
						</thead>
        </DataTable>
  );
};
