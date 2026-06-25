import { SitesTable } from "./SitesTable";
import { useEffect, useRef } from "react";
import { useSearchParams } from "react-router-dom";
import {
  useDeleteSiteMutation,
  useFetchSite,
  useSaveSiteMutation,
  useSitesQuery,
} from "../../queries/sites";
import type { AppDataTableHandle } from "../../components/data-table";

export const SitesPage: React.FC = () => {
  const { data: sites = [], isFetching } = useSitesQuery();
  const fetchSite = useFetchSite();
  const saveSite = useSaveSiteMutation();
  const deleteSite = useDeleteSiteMutation();

  // Deep-link: /sites?siteId=123 opens that site in edit mode once it's loaded.
  // Tracked so we only honor the param on first match; subsequent navigation
  // away/back is the user's responsibility.
  const tableRef = useRef<AppDataTableHandle>(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const consumedDeepLinkRef = useRef(false);
  useEffect(() => {
    if (consumedDeepLinkRef.current) return;
    const raw = searchParams.get("siteId");
    if (!raw) return;
    const targetId = Number(raw);
    if (!Number.isFinite(targetId)) return;
    if (!sites.some((s) => s.siteId === targetId)) return;
    consumedDeepLinkRef.current = true;
    tableRef.current?.openRow(targetId, "edit");
    const next = new URLSearchParams(searchParams);
    next.delete("siteId");
    setSearchParams(next, { replace: true });
  }, [searchParams, setSearchParams, sites]);

  return (
    <SitesTable
      sites={sites}
      loading={isFetching}
      getSite={fetchSite}
      actions={{
        save: (site) => saveSite.mutateAsync(site),
        remove: (siteId) => deleteSite.mutate(siteId),
      }}
      tableRef={tableRef}
    />
  );
};
