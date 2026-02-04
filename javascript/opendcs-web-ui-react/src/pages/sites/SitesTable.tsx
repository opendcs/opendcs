import DataTable, {
  type DataTableProps,
  type DataTableRef,
  type DataTableSlots,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import { useTranslation } from "react-i18next";
import { useEffect, useMemo, useRef, useState } from "react";
import { dtLangs } from "../../lang";
import type {
  ApiSite,
  ApiSiteRef,
} from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import Site from "./Site";
// import { createRoot } from "react-dom/client";
// import RefListContext, { useRefList } from "../../contexts/data/RefListContext";
import type { SaveAction, UiState } from "../../util/Actions";
import { useContextWrapper } from "../../util/ContextWrapper";
import { Button } from "react-bootstrap";
import { Pencil } from "react-bootstrap-icons";
import type { RowState } from "../../util/DataTables";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export type TableSiteRef = Partial<ApiSiteRef>;

export interface SiteTableProperties {
  sites: TableSiteRef[];
  getSite?: (siteId: number) => Promise<ApiSite | undefined>;
  actions?: SaveAction<ApiSite>;
}

export const SitesTable: React.FC<SiteTableProperties> = ({
  sites,
  getSite,
  actions = {},
}) => {
  // Note entirely sure how I feel about this but it appears to primarily be a limitation
  // of the interactions of React with DataTables since DataTables has to control this DOM node
  // So we are doing a lot of "I want to render a react component but need it to be a DomNode"
  // which as the wonderful affect of breaking out of the context tree
  const { toDom } = useContextWrapper();
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["sites"]);
  const [localSites, updateLocalSites] = useState<TableSiteRef[]>([]);
  const [rowState, updateRowState] = useState<RowState<number>>({});

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
      actions: (data: TableSiteRef, _row: any) => {
        const id: number = data.siteId!;
        const inEdit = rowState![id] === "edit" || rowState[id] === "new";
        return !inEdit ? (
          <Button
            variant="warning"
            onClick={(e) => {
              e.stopPropagation();
              updateRowState((prev) => {
                return {
                  ...prev,
                  [id]: "edit",
                };
              });
            }}
            aria-label={t("sites:edit_site", { id: id })}
          >
            <Pencil />
          </Button>
        ) : null;
      },
    };
  }, [rowState, i18n.language]);

  const options: DataTableProps["options"] = {
    paging: true,
    responsive: true,
    stateSave: true,
    language: dtLangs.get(i18n.language),
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    createdRow: (_row, _data, dataIndex) => {
      // this prevent clicks on elements within the child row from triggering the handler defined in the useEffect below
      table.current?.dt()?.row(dataIndex).node().classList.add("child-toggle");
    },
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
                updateRowState((prevRowState) => {
                  return {
                    ...prevRowState,
                    [newId]: "new",
                  };
                });
                return [...prev, { siteId: newId }];
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

  const renderSite = (data: TableSiteRef, edit: boolean = false): Node => {
    const site =
      data.siteId && data.siteId > 0 ? getSite!(data.siteId!) : Promise.resolve({});
    const workingSite = rowState[data.siteId!] === "new" ? {} : site; // maybe put in "row state"?

    const container = toDom(
      <Site
        site={workingSite}
        actions={{
          save: (site: ApiSite) => {
            actions.save!(site); // todo: error handling, plus the hold "update the row thing"
            updateLocalSites((prev) => {
              return prev.filter((ps) => ps.siteId !== site.siteId);
            });
            updateRowState((prev) => {
              const { [site.siteId!]: _, ...states } = prev;
              return {
                ...states,
                [site.siteId!]: "show",
              };
            });
          },
          cancel: (item) => {
            updateRowState((prev) => {
              const { [item]: _, ...states } = prev;
              return {
                ...states,
                [item]: "show",
              };
            });
          },
        }}
        edit={edit}
      />,
    );
    return container;
  };

  useEffect(() => {
    // Add event listener for opening and closing details
    table.current?.dt()?.on("click", "tbody tr.child-toggle", function (e) {
      if (getSite === undefined) {
        return; // do nothing, we can't look it up.
      }
      const target = e.target! as Element;
      const tr = target.closest("tr");
      if (tr?.classList.contains("child-row")) {
        return; // don't do anything if we click the child row.
      }
      const dt = table.current!.dt()!;

      const row = dt.row(tr as HTMLTableRowElement);
      updateRowState((prev) => {
        const idx = (row.data() as TableSiteRef).siteId!;
        const { [idx]: existing, ...remaining } = prev;
        let newValue: UiState = "show";
        if (existing !== undefined) {
          newValue = undefined;
        }
        return {
          ...remaining,
          [idx]: newValue,
        };
      });
    });
  }, [i18n.language]);

  useEffect(() => {
    if (table.current?.dt()) {
      const dt = table.current.dt()!;
      const visibleRows = dt.rows({ page: "current", search: "applied" });
      visibleRows.every(function () {
        const row = this;
        const idx = (row.data() as TableSiteRef).siteId!;
        //row.invalidate();
        if (rowState[idx] !== undefined) {
          //row.child()?.hide();
          const data: TableSiteRef = row.data() as TableSiteRef;
          const edit = rowState[idx] !== "show";
          row.child(renderSite(data, edit), "child-row").show();
        } else {
          row.child()?.hide();
        }
        //row.draw(false);
      });
    }
  }, [rowState, sites, siteData]);

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
