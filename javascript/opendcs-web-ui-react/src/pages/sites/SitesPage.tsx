import { ApiSite, RESTDECODESSiteRecordsApi, type ApiSiteRef } from "opendcs-api";
import { SitesTable } from "./SitesTable";
import { useCallback, useEffect, useState } from "react";
import { useApi } from "../../contexts/app/ApiContext";

export const SitesPage: React.FC = () => {
  const [sites, updateSites] = useState<ApiSiteRef[]>([]);
  const [stale, setStale] = useState(true);
  const api = useApi();
  const sitesApi = new RESTDECODESSiteRecordsApi(api.conf);

  useEffect(() => {
    const fetchSites = async () => {
      const sites = await sitesApi.getsiterefs(api.org);
      updateSites(sites);
      setStale(false);
    };
    if (stale === true) {
      fetchSites();
    }
  }, [stale]);

  const getSite = useCallback(
    (siteId: number) => {
      return sitesApi.getsite(api.org, siteId!);
    },
    [api],
  );

  const saveSite = useCallback(
    (site: ApiSite) => {
      sitesApi.postsite(api.org, site);
      setStale(true);
    },
    [api],
  );

  const deleteSite = useCallback(
    (siteId: number) => {
      sitesApi.deletesite(api.org, siteId);
      setStale(true);
    },
    [api],
  );

  return (
    <SitesTable
      sites={sites}
      getSite={getSite}
      actions={{ save: saveSite, remove: deleteSite }}
    />
  );
};
