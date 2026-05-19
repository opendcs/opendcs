import { Suspense, use, type ReactNode } from "react";
import { createRoot } from "react-dom/client";
import RefListContext from "../contexts/data/RefListContext";
import { I18nextProvider, useTranslation } from "react-i18next";
import { AuthContext } from "../contexts/app/AuthContext";
import { ThemeContext } from "../contexts/app/ThemeContext";
import { ApiContext } from "../contexts/app/ApiContext";
import { QueryClientProvider, useQueryClient } from "@tanstack/react-query";

export interface Wrappers {
  /**
   * Renders all provided children into a new React Root done inside a div that is created.
   * Primiarly used to work around the limitations of DataTable's render methods most
   * of which do not take a ReactNode.
   * @param children
   * @returns
   */
  toDom: (children: ReactNode) => Node;
}

/**
 * Primarly for use in DataTables renders to allow sharing application contexts as needed
 * when raw DOM Nodes are required. This is due to a limitation of DataTables.
 *
 * If you need to use the appliation context in a region that just won't integrate into react
 * the way we desire, your component can pull the methods from this hook to work that magic.
 *
 * Use sparingly.
 */
export function useContextWrapper(): Wrappers {
  const refContext = use(RefListContext);
  const authContext = use(AuthContext);
  const themeContext = use(ThemeContext);
  const apiContext = use(ApiContext);
  const queryClient = useQueryClient();
  const { i18n } = useTranslation();

  return {
    toDom: (children: ReactNode): Node => {
      const container = document.createElement("div");
      const root = createRoot(container);
      // The DataTables-rendered subtree gets a fresh React root, so contexts
      // from the parent tree don't flow in automatically. Re-wrap with the
      // same context values (and the same QueryClient) so any TanStack hooks
      // used downstream share the parent's cache.
      root.render(
        <I18nextProvider i18n={i18n}>
          <ThemeContext value={themeContext}>
            <ApiContext value={apiContext}>
              <AuthContext value={authContext}>
                <RefListContext value={refContext}>
                  <QueryClientProvider client={queryClient}>
                    <Suspense fallback="Loading...">{children}</Suspense>
                  </QueryClientProvider>
                </RefListContext>
              </AuthContext>
            </ApiContext>
          </ThemeContext>
        </I18nextProvider>,
      );
      return container;
    },
  };
}
