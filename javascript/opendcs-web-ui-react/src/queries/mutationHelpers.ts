import type { QueryClient, UseMutationOptions } from "@tanstack/react-query";

// Shared helpers for the per-entity query modules (queries/*.ts). These collapse
// two patterns that were otherwise copy-pasted into every entity's save/delete
// mutation.

/**
 * The root key for one entity within one organization, as produced by the
 * `<entity>Keys.all(org)` factories in keys.ts — e.g. `["sites", "acme"]`.
 * Invalidating it cascades to that entity's list and detail queries for that
 * org only, and never crosses tenants (a different org has a different tuple).
 */
export type EntityRootKey = readonly [entity: string, org: string];

/**
 * `onSuccess` shared by every save/delete mutation: invalidate the entity's
 * org-scoped root key (cascading to its list + detail queries), then delegate
 * to any caller-supplied `onSuccess`.
 *
 * The generics default so the mutation's own `TVariables`/`TData` are inferred
 * at the call site instead of collapsing to `unknown`.
 *
 * @example
 * useMutation({
 *   mutationFn: (site: ApiSite) => sitesApi.postsite(org, site),
 *   onSuccess: invalidateThenDelegate(queryClient, siteKeys.all(org), options?.onSuccess),
 * });
 */
export const invalidateThenDelegate =
  <TData = unknown, TError = unknown, TVariables = unknown, TContext = unknown>(
    queryClient: QueryClient,
    rootKey: EntityRootKey,
    onSuccess?: UseMutationOptions<TData, TError, TVariables, TContext>["onSuccess"],
  ): NonNullable<
    UseMutationOptions<TData, TError, TVariables, TContext>["onSuccess"]
  > =>
  async (...args) => {
    await queryClient.invalidateQueries({ queryKey: rootKey });
    onSuccess?.(...args);
  };

/**
 * Normalizes a server-assigned numeric id before a POST: a "new" record carries
 * a missing or non-positive id (0 or a transient negative), which we send as
 * `undefined` so the server assigns the real id. An existing record's positive
 * id is passed through unchanged.
 *
 * @example
 * appApi.postApp(org, { ...app, appId: normalizeNewId(app.appId) });
 */
export const normalizeNewId = (id: number | undefined): number | undefined =>
  id && id > 0 ? id : undefined;
