import {
  Suspense,
  useSyncExternalStore,
  type ContextType,
  type ReactNode,
} from "react";
import { I18nextProvider } from "react-i18next";
import { QueryClientProvider, type QueryClient } from "@tanstack/react-query";
import type { i18n } from "i18next";
import RefListContext from "../contexts/data/RefListContext";
import { AuthContext } from "../contexts/app/AuthContext";
import { ThemeContext } from "../contexts/app/ThemeContext";
import { ApiContext } from "../contexts/app/ApiContext";
import { SiteNameTypeContext } from "../contexts/app/SiteNameTypeContext";

/** The application contexts handed on to every root `toDom` creates. */
export interface SharedContexts {
  refContext: ContextType<typeof RefListContext>;
  authContext: ContextType<typeof AuthContext>;
  themeContext: ContextType<typeof ThemeContext>;
  apiContext: ContextType<typeof ApiContext>;
  siteNameTypeContext: ContextType<typeof SiteNameTypeContext>;
  queryClient: QueryClient;
  i18n: i18n;
}

/** Latest context values of the tree that owns the `toDom` roots. */
export interface ContextStore {
  get: () => SharedContexts;
  set: (value: SharedContexts) => void;
  subscribe: (listener: () => void) => () => void;
}

interface SharedContextProvidersProps {
  store: ContextStore;
  children: ReactNode;
}

// The DataTables-rendered subtree gets a fresh React root, so contexts from
// the parent tree don't flow in automatically. Re-wrap with the same context
// values (and the same QueryClient) so any TanStack hooks used downstream
// share the parent's cache. The values are read from the store rather than
// captured, because these roots outlive the render that created them: a row
// opened while the reference lists were still loading must see them arrive.
export const SharedContextProviders = ({
  store,
  children,
}: SharedContextProvidersProps) => {
  const contexts = useSyncExternalStore(store.subscribe, store.get);
  return (
    <I18nextProvider i18n={contexts.i18n}>
      <ThemeContext value={contexts.themeContext}>
        <ApiContext value={contexts.apiContext}>
          <AuthContext value={contexts.authContext}>
            <RefListContext value={contexts.refContext}>
              <SiteNameTypeContext value={contexts.siteNameTypeContext}>
                <QueryClientProvider client={contexts.queryClient}>
                  <Suspense fallback="Loading...">{children}</Suspense>
                </QueryClientProvider>
              </SiteNameTypeContext>
            </RefListContext>
          </AuthContext>
        </ApiContext>
      </ThemeContext>
    </I18nextProvider>
  );
};

export default SharedContextProviders;
