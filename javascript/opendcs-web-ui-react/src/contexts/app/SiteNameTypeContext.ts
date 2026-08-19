import { createContext, useContext } from "react";

/**
 * Site name "types" (enumerators) a DECODES site can have a name in, e.g. a
 * USGS site may have both a USGS number and a CWMS location id. Mirrors the
 * fixed set of types the DECODES database understands, see
 * decodes.db.Constants#snt_*.
 */
export const SITE_NAME_TYPES = [
  "CWMS",
  "NWSHB5",
  "USGS",
  "USGS-DRGS",
  "Local",
  "nos",
] as const;

export type SiteNameType = (typeof SITE_NAME_TYPES)[number];

/** Empty string means "no preference" - fall back to whatever the server provides. */
export type SiteNameTypePreference = SiteNameType | "";

export interface SiteNameTypeSetting {
  preferredType: SiteNameTypePreference;
}

export interface SiteNameTypeContextType {
  siteNameType: SiteNameTypeSetting;
  setSiteNameType: React.Dispatch<React.SetStateAction<SiteNameTypeSetting>>;
}

export const SiteNameTypeContext = createContext<SiteNameTypeContextType | undefined>(
  undefined,
);

export const useSiteNameType = () => {
  const context = useContext(SiteNameTypeContext);
  if (context == undefined) {
    throw new Error("SiteNameType isn't defined?");
  }
  return context;
};
