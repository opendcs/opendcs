export const siteDisplayName = (site: {
  siteId?: number;
  publicName?: string | null;
  sitenames?: { [key: string]: string };
}): string =>
  site.publicName ||
  (site.sitenames ? Object.values(site.sitenames)[0] : undefined) ||
  String(site.siteId ?? "");
