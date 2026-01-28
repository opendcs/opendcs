import DataTable, {
  type DataTableProps,
  type DataTableRef,
  type DataTableSlots,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import { useTranslation } from "react-i18next";
import { Suspense, useEffect, useMemo, useRef, useState } from "react";
import { dtLangs } from "../../lang";
import type { ApiSiteRef } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import Site, { type UiSite } from "./Site";
// import { createRoot } from "react-dom/client";
// import RefListContext, { useRefList } from "../../contexts/data/RefListContext";
import type { CollectionActions, UiState } from "../../util/Actions";
import { useContextWrapper } from "../../util/ContextWrapper";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

type TableSiteRef = Partial<ApiSiteRef & { state?: UiState; actualSite?: UiSite }>;

interface SiteTableProperties {
  sites: TableSiteRef[];
  // NOTE: primarily used for testing as there is no way to inject this data wise,
  newSites?: TableSiteRef[];
  actions?: CollectionActions<ApiSiteRef, number>;
}

export const SitesTable: React.FC<SiteTableProperties> = ({
  sites,
  newSites = [],
  actions = {},
}) => {
  // Note entirely sure how I feel about this but it appears to primarily be a limitation
  // of the interactions of React with DataTables since DataTables has to control this DOM node
  // So we are doing a lot of "I want to render a react component but need it to be a DomNode"
  // which as the wonderful affect of breaking out of the context tree
  const { toDom } = useContextWrapper();
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["sites"]);
  const [localSites, updateLocalSites] = useState(newSites);

  const siteData = useMemo(() => [...sites, ...localSites], [sites, localSites]);

  const columns = [
    { data: "siteId", defaultContent: "new" },
    {
      data: null,
      render: (_data: unknown, _type: unknown, row: unknown) => {
        const site = row as ApiSiteRef;
        return site.sitenames?.CWMS || ""; // TODO need configured sitename preference
      },
    },
    { data: "publicName", defaultContent: "" },
    { data: "description", defaultContent: "" },
    { data: null, name: "actions" },
  ];

  const slots = useMemo<DataTableSlots>(() => {
    return {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      actions: (data: TableSiteRef, type: unknown, _row: TableSiteRef) => {
        if (type === "display") {
          const inEdit = data.state !== undefined;
          return inEdit ? "edit actions" : "default actions";
        } else {
          return data;
        }
      },
    };
  }, []);

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
              updateLocalSites((prev) => {
                // modal here for sitename?... or focus on automatically created new entry?
                let existing = prev
                  .map((v) => v.siteId!)
                  // we want the lowest
                  .sort((a: number, b: number) => {
                    return b - a;
                  });
                const newId = existing.length > 0 ? existing[0] - 1 : -1; // use negative indexes for new elements
                return [...prev, { state: "new", actualSite: {}, siteId: newId }];
              });
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
        // TODO: need to lookup data. Given use of suspense, should be able to pass a promise
        const data: TableSiteRef = row.data as TableSiteRef;
        const emptySite: UiSite = {};
        const container = toDom(
          <Site
            site={data.state === "new" ? data.actualSite! : emptySite}
            actions={{ save: actions.save }}
          />,
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
      data={siteData}
      options={options}
      slots={slots}
      ref={table}
      className="table table-hover table-striped tablerow-cursor w-100 border"
    >
      <caption className="captionTitleCenter">{t("sites:title")}</caption>
      <thead>
        <tr>
          <th>{t("sites:site_id")}</th>
          <th id="siteNameColumnHeader">{t("sites:site_name")}</th>
          <th>{t("sites:public_name")}</th>
          <th>{t("sites:description")}</th>
          <th>{t("translation:actions")}</th>
        </tr>
      </thead>
    </DataTable>
  );
};
