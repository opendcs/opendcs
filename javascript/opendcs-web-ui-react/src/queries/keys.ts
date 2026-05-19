// Centralized query-key factories. Every key starts with the entity name
// followed by the org, so invalidating <entity>Keys.all(org) wipes only that
// org's data and never leaks across tenants.
export const siteKeys = {
  all: (org: string) => ["sites", org] as const,
  list: (org: string) => [...siteKeys.all(org), "list"] as const,
  detail: (org: string, siteId: number) =>
    [...siteKeys.all(org), "detail", siteId] as const,
};

export const algorithmKeys = {
  all: (org: string) => ["algorithms", org] as const,
  list: (org: string) => [...algorithmKeys.all(org), "list"] as const,
  detail: (org: string, algorithmId: number) =>
    [...algorithmKeys.all(org), "detail", algorithmId] as const,
  propSpecs: (org: string, execClass: string) =>
    [...algorithmKeys.all(org), "propSpecs", execClass] as const,
};

export const platformKeys = {
  all: (org: string) => ["platforms", org] as const,
  list: (org: string) => [...platformKeys.all(org), "list"] as const,
  detail: (org: string, platformId: number) =>
    [...platformKeys.all(org), "detail", platformId] as const,
  config: (org: string, configId: number) =>
    [...platformKeys.all(org), "config", configId] as const,
};

export const computationKeys = {
  all: (org: string) => ["computations", org] as const,
  list: (org: string) => [...computationKeys.all(org), "list"] as const,
  detail: (org: string, computationId: number) =>
    [...computationKeys.all(org), "detail", computationId] as const,
};

export const appKeys = {
  all: (org: string) => ["apps", org] as const,
  list: (org: string) => [...appKeys.all(org), "list"] as const,
};

export const tsGroupKeys = {
  all: (org: string) => ["tsGroups", org] as const,
  list: (org: string) => [...tsGroupKeys.all(org), "list"] as const,
};

export const refListKeys = {
  all: (org: string) => ["refLists", org] as const,
  list: (org: string) => [...refListKeys.all(org), "list"] as const,
};

export const unitKeys = {
  all: (org: string) => ["units", org] as const,
  list: (org: string) => [...unitKeys.all(org), "list"] as const,
  conversions: (org: string) => [...unitKeys.all(org), "conversions"] as const,
};

// Org list is global (not scoped to an org) — it's the source of truth that
// drives the org switcher itself.
export const orgKeys = {
  all: () => ["orgs"] as const,
  list: () => [...orgKeys.all(), "list"] as const,
};
