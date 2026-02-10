import { ApiSite, RESTDECODESSiteRecordsApi, type ApiSiteRef } from "opendcs-api";
import { SitesTable } from "./SitesTable";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useApi } from "../../contexts/app/ApiContext";

export const SitesPage: React.FC = () => {
  const [sites, setSites] = useState<ApiSiteRef[]>([]);
  const [stale, setStale] = useState(true);
  const api = useApi();
  const sitesApi = useMemo(() => new RESTDECODESSiteRecordsApi(api.conf), [api.conf]);

  useEffect(() => {
    const fetchSites = async () => {
      const sites = await sitesApi.getsiterefs(api.org);
      setSites(sites);
      setStale(false);
    };
    if (stale === true) {
      fetchSites();
    }
  }, [stale, api.org, sitesApi]);

  const getSite = useCallback(
    (siteId: number) => {
      return sitesApi.getsite(api.org, siteId);
    },
    [api.org, sitesApi],
  );

  const saveSite = useCallback(
    (site: ApiSite) => {
      sitesApi.postsite(api.org, site).then(() => setStale(true));
    },
    [api.org, sitesApi],
  );

  const deleteSite = useCallback(
    (siteId: number) => {
      sitesApi.deletesite(api.org, siteId).then(() => setStale(true));
    },
    [api.org, sitesApi],
  );

  return (
    <SitesTable
      sites={sites}
      getSite={getSite}
      actions={{ save: saveSite, remove: deleteSite }}
    />
  );
};
