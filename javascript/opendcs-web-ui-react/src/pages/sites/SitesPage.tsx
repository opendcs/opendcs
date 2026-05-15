import { ApiSite, RESTDECODESSiteRecordsApi, type ApiSiteRef } from "opendcs-api";
import { SitesTable } from "./SitesTable";
import { useCallback, useEffect, useMemo, useRef } from "react";
import { useSearchParams } from "react-router-dom";
import { useApi } from "../../contexts/app/ApiContext";
import { useStaleFetch } from "../../hooks/useStaleFetch";
import type { AppDataTableHandle } from "../../components/data-table";

export const SitesPage: React.FC = () => {
  const api = useApi();
  const sitesApi = useMemo(() => new RESTDECODESSiteRecordsApi(api.conf), [api.conf]);

  const fetchSiteRefs = useCallback(
    () => sitesApi.getsiterefs(api.org),
    [sitesApi, api.org],
  );
  const {
    data: sites,
    loading,
    refresh,
  } = useStaleFetch<ApiSiteRef[]>(fetchSiteRefs, []);

  const getSite = useCallback(
    (siteId: number) => {
      return sitesApi.getsite(api.org, siteId);
    },
    [api.org, sitesApi],
  );

  const saveSite = useCallback(
    (site: ApiSite) => {
      sitesApi.postsite(api.org, site).then(() => refresh());
    },
    [api.org, sitesApi, refresh],
  );

  const deleteSite = useCallback(
    (siteId: number) => {
      sitesApi.deletesite(api.org, siteId).then(() => refresh());
    },
    [api.org, sitesApi, refresh],
  );

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
      loading={loading}
      getSite={getSite}
      actions={{ save: saveSite, remove: deleteSite }}
      tableRef={tableRef}
    />
  );
};
