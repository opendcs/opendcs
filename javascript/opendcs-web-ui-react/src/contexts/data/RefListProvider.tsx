import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { RefListContext, type RefListContextType } from "./RefListContext";
import { useApi } from "../app/ApiContext";
import { Record } from "react-bootstrap-icons";
import { RESTReferenceListsApi, type ApiRefList } from "opendcs-api";

interface ProviderProps {
  children: ReactNode;
}

export const RefListProvider = ({ children }: ProviderProps) => {
  const api = useApi();
  const [refLists, setRefLists] = useState<Record<string, ApiRefList>>({});
  const [ready, setReady] = useState(false);
  const refListsRef = useRef(refLists);

  useEffect(() => {
    const fetchLists = async () => {
      const refListApi = new RESTReferenceListsApi(api.conf);
      const refs = await refListApi.getRefLists(api.org);
      setRefLists(refs);
      setReady(true);
    };
    fetchLists();
  }, []);

  useEffect(() => {
    refListsRef.current = refLists;
  }, [refLists]);

  const getRefList = useCallback(
    (list: string) => {
      return refListsRef.current[list];
    },
    [refLists, refListsRef],
  );

  const refListvalue: RefListContextType = {
    refList: getRefList,
    ready: ready,
  };

  return <RefListContext value={refListvalue}>{children}</RefListContext>;
};
