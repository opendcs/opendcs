import { useCallback, type ReactNode } from "react";
import { RefListContext, type RefListContextType } from "./RefListContext";
import { useRefListsQuery } from "../../queries/refLists";

interface ProviderProps {
  children: ReactNode;
}

// Thin context wrapper over useRefListsQuery so the existing
// useRefList()/refList(name) consumer API stays unchanged. Cache + org-scoping
// + refetch-on-org-switch come from TanStack — the prior implementation kept
// an empty `[]` dep array and never refetched when the user switched orgs.
export const RefListProvider = ({ children }: ProviderProps) => {
  const { data, isSuccess } = useRefListsQuery();
  const refList = useCallback((name: string) => data?.[name] ?? {}, [data]);
  const value: RefListContextType = { refList, ready: isSuccess };
  return <RefListContext value={value}>{children}</RefListContext>;
};
