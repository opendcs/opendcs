import { useCallback, useEffect, useState } from "react";

/**
 * Fetches data lazily and exposes a refresh() to re-run the fetch.
 *
 * Guards against React StrictMode's double-mount in development: if the
 * component unmounts before the fetch resolves, the resolved result is
 * discarded rather than written into stale state.
 *
 * The fetcher must be stable across renders (wrap it in useCallback),
 * otherwise the effect will re-run on every render.
 */
export function useStaleFetch<T>(
  fetcher: () => Promise<T>,
  initial: T,
): { data: T; loading: boolean; refresh: () => void } {
  const [data, setData] = useState<T>(initial);
  const [stale, setStale] = useState(true);

  useEffect(() => {
    if (!stale) return;
    let cancelled = false;
    fetcher()
      .then((result) => {
        if (cancelled) return;
        setData(result);
        setStale(false);
      })
      .catch((e: unknown) => {
        // Swallow rejections from the abandoned StrictMode double-mount so
        // they don't surface as "Uncaught (in promise)" noise; let real
        // failures from the active mount propagate.
        if (cancelled) return;
        throw e;
      });
    return () => {
      cancelled = true;
    };
  }, [stale, fetcher]);

  const refresh = useCallback(() => setStale(true), []);

  return { data, loading: stale, refresh };
}
