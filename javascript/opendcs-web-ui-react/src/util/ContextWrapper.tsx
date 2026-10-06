import { use, useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { createRoot, type Root } from "react-dom/client";
import RefListContext from "../contexts/data/RefListContext";
import { useTranslation } from "react-i18next";
import { AuthContext } from "../contexts/app/AuthContext";
import { ThemeContext } from "../contexts/app/ThemeContext";
import { ApiContext } from "../contexts/app/ApiContext";
import { SiteNameTypeContext } from "../contexts/app/SiteNameTypeContext";
import { useQueryClient } from "@tanstack/react-query";
import {
  SharedContextProviders,
  type ContextStore,
  type SharedContexts,
} from "./SharedContextProviders";

const rootsByContainer = new WeakMap<Node, Root>();

/** Unmount the React root created by `toDom` for `node`, if any. Safe to
 *  call on nodes that were never a `toDom` container. */
export function unmountToDom(node: Node): void {
  const root = rootsByContainer.get(node);
  if (!root) return;
  rootsByContainer.delete(node);
  root.unmount();
}

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

function createContextStore(initial: SharedContexts): ContextStore {
  let current = initial;
  const listeners = new Set<() => void>();
  return {
    get: () => current,
    set: (value) => {
      if (value === current) return;
      current = value;
      listeners.forEach((listener) => listener());
    },
    subscribe: (listener) => {
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
      };
    },
  };
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
  const siteNameTypeContext = use(SiteNameTypeContext);
  const queryClient = useQueryClient();
  const { i18n } = useTranslation();

  const contexts: SharedContexts = useMemo(
    () => ({
      refContext,
      authContext,
      themeContext,
      apiContext,
      siteNameTypeContext,
      queryClient,
      i18n,
    }),
    [
      refContext,
      authContext,
      themeContext,
      apiContext,
      siteNameTypeContext,
      queryClient,
      i18n,
    ],
  );
  // The roots made by `toDom` are never re-rendered from here, so they follow
  // the contexts through this store instead of keeping the values they were
  // created with.
  const [store] = useState(() => createContextStore(contexts));
  useEffect(() => {
    store.set(contexts);
  }, [store, contexts]);

  const toDom = useCallback(
    (children: ReactNode): Node => {
      const container = document.createElement("div");
      const root = createRoot(container);
      rootsByContainer.set(container, root);
      root.render(
        <SharedContextProviders store={store}>{children}</SharedContextProviders>,
      );
      return container;
    },
    [store],
  );

  return { toDom };
}
