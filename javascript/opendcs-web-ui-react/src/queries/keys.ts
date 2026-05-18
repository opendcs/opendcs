// Centralized query-key factories. Every key starts with the entity name
// followed by the org, so invalidating siteKeys.all(org) wipes only that
// org's site data and never leaks across tenants.
export const siteKeys = {
  all: (org: string) => ["sites", org] as const,
  list: (org: string) => [...siteKeys.all(org), "list"] as const,
  detail: (org: string, siteId: number) =>
    [...siteKeys.all(org), "detail", siteId] as const,
};
