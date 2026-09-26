/**
 * Cache policy for reference data - units, data types, intervals, ref lists,
 * orgs. These are small, effectively static per org, and feed the dropdowns
 * that every editor renders.
 *
 * `gcTime` must be at least as long as `staleTime`: it is measured from the
 * moment the last observer unmounts, so a shorter `gcTime` silently evicts data
 * the query still considers fresh. With the QueryClient default of 5 minutes,
 * navigating away from a page for longer than that dropped these lists, and the
 * consumers that fall back when the data is missing (UnitSelect renders a plain
 * text box, the data-type-standard select renders a short option list) came back
 * degraded on the next visit - see issue #2052.
 */
export const REFERENCE_DATA_CACHE = {
  staleTime: 60 * 60_000,
  gcTime: 60 * 60_000,
} as const;
