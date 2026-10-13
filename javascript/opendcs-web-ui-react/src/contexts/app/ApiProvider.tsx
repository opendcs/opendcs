import { useEffect, useState, type ReactNode } from "react";
import { ApiContext, defaultValue } from "./ApiContext";
import { ApiOrganization } from "opendcs-api";

interface ProviderProps {
  children: ReactNode;
}

export const ApiProvider = ({ children }: ProviderProps) => {
  const [org, setOrg] = useState<ApiOrganization>(() => {
    const localOrg = JSON.parse(window.localStorage.getItem("organization") || "{}");
    return localOrg;
  });

  useEffect(() => {
    window.localStorage.setItem("organization", JSON.stringify(org));
  }, [org]);

  return (
    <ApiContext
      value={{ ...defaultValue, org: org.name || "", orgObj: org, setOrg: setOrg }}
    >
      {children}
    </ApiContext>
  );
};
