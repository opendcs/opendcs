import { ApiSite, RESTDECODESSiteRecordsApi, type ApiSiteRef } from "opendcs-api";
import { SitesTable } from "./SitesTable";
import { useCallback, useMemo } from "react";
import { useApi } from "../../contexts/app/ApiContext";
import { useStaleFetch } from "../../hooks/useStaleFetch";

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

  return (
    <SitesTable
      sites={sites}
      loading={loading}
      getSite={getSite}
      actions={{ save: saveSite, remove: deleteSite }}
    />
  );
};
