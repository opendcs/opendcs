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
  const [refLists, updateRefLists] = useState<Record<string, ApiRefList>>({});
  const refListsRef = useRef(refLists);

  useEffect(() => {
    const fetchLists = async () => {
      const refListApi = new RESTReferenceListsApi(api.conf);
      const refs = await refListApi.getRefLists(api.org || "");
      updateRefLists(refs);
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
  };

  return <RefListContext value={refListvalue}>{children}</RefListContext>;
};
