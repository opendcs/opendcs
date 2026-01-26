import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import { useTranslation } from "react-i18next";
import { Suspense, useEffect, useRef } from "react";
import { dtLangs } from "../../lang";
import type { ApiSite } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import Site from "./Site";
import { createRoot } from "react-dom/client";
import RefListContext, { useRefList } from "../../contexts/data/RefListContext";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

interface SiteTableProperties {
  sites: ApiSite[];
}

export const SitesTable: React.FC<SiteTableProperties> = ({ sites }) => {
  // Note entirely sure how I feel about this but it appears to primarily be a limitation
  // of the interactions of React with DataTables since DataTables has to control this DOM node
  // So we are doing a lot of "I want to render a react component but need it to be a DomNode"
  // which as the wonderful affect of breaking out of the context tree
  const refContext = useRefList();
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["sites"]);

  const columns = [
    { data: "siteId" },
    {
      data: null,
      render: (_data: unknown, _type: unknown, row: unknown) => {
        const site = row as ApiSite;
        return site.sitenames?.CWMS;
      },
    },
    { data: "description", defaultContent: "" },
    {
      data: null,
      name: "actions",
      render: () => {
        return "action";
      },
    },
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
              console.log("Add site");
            },
            attr: {
              "aria-label": t("sites:add_site"),
            },
          },
        ],
      },
    },
  };

  useEffect(() => {
    // Add event listener for opening and closing details
    table.current?.dt()!.on("click", "tbody td", function (e) {
      const dt = table.current!.dt()!;
      const target = e.target! as Element;
      const tr = target.closest("tr");
      if (tr?.classList.contains("child-row")) {
        return; // don't do anything if we click the child row.
      }
      const row = dt.row(tr as HTMLTableRowElement);

      if (row.child.isShown()) {
        // This row is already open - close it
        row.child.hide();
      } else {
        const data: ApiSite = row.data as ApiSite;
        const container = document.createElement("div");
        const root = createRoot(container);
        root.render(
          <RefListContext value={refContext}>
          <Suspense fallback="Loading...">
            <Site site={data} />
          </Suspense>
          </RefListContext>,
        );
        // Open this row
        row.child(container, "child-row").show();
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
      <caption className="captionTitleCenter">{t("sites:title")}</caption>
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
