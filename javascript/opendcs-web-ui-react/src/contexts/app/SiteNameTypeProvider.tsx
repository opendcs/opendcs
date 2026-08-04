import { useEffect, useState, type ReactNode } from "react";
import {
  SiteNameTypeContext,
  type SiteNameTypePreference,
  type SiteNameTypeSetting,
} from "./SiteNameTypeContext";

const STORAGE_KEY = "site-name-type-preference";

interface ProviderProps {
  children: ReactNode;
}

export const SiteNameTypeProvider = ({ children }: ProviderProps) => {
  const stored = (localStorage.getItem(STORAGE_KEY) as SiteNameTypePreference) || "";

  const [siteNameType, setSiteNameType] = useState<SiteNameTypeSetting>({
    preferredType: stored,
  });

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, siteNameType.preferredType);
  }, [siteNameType]);

  return (
    <SiteNameTypeContext value={{ siteNameType, setSiteNameType }}>
      {children}
    </SiteNameTypeContext>
  );
};
