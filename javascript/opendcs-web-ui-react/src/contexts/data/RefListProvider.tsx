import { useCallback, useMemo, type ReactNode } from "react";
import { RefListContext, type RefListContextType } from "./RefListContext";
import { useRefListsQuery } from "../../queries/refLists";
import { useAuth } from "../app/AuthContext";

interface ProviderProps {
  children: ReactNode;
}

// Thin context wrapper over useRefListsQuery so the existing
// useRefList()/refList(name) consumer API stays unchanged. Cache + org-scoping
// + refetch-on-org-switch come from TanStack — the prior implementation kept
// an empty `[]` dep array and never refetched when the user switched orgs.
export const RefListProvider = ({ children }: ProviderProps) => {
  // This provider sits above the login page, and /reflists needs a session.
  // Asked before sign-in the request is rejected and nothing asks again, which
  // left every reference list dropdown empty until the page was reloaded.
  const { user } = useAuth();
  const { data, isSuccess, isError } = useRefListsQuery(user !== undefined);
  const refList = useCallback((name: string) => data?.[name] ?? {}, [data]);
  const value: RefListContextType = useMemo(
    () => ({ refList, ready: isSuccess, failed: isError }),
    [refList, isSuccess, isError],
  );
  return <RefListContext value={value}>{children}</RefListContext>;
};
