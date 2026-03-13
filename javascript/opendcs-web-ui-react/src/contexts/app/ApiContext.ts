import { ApiOrganization, type Configuration } from "opendcs-api";
import { createConfiguration, ServerConfiguration } from "opendcs-api";
import { createContext, useContext, type SetStateAction } from "react";
import { parseOrg } from "./OrganizationsContext.ts";

export interface ApiContextType {
  conf: Configuration;
  org: string;
  orgObj: ApiOrganization;
  setOrg: React.Dispatch<React.SetStateAction<ApiOrganization>>;
}

export const defaultValue: ApiContextType = {
  conf: createConfiguration({
    baseServer: new ServerConfiguration("/odcsapi", {}),
  }),
  org: parseOrg(window.localStorage.getItem("organization")).name || "",
  orgObj: parseOrg(window.localStorage.getItem("organization")) || {},
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  setOrg: (_value: SetStateAction<ApiOrganization>): void => {
    window.localStorage.setItem("organization", JSON.stringify(_value));
  },
};

export const ApiContext = createContext<ApiContextType>(defaultValue);

export const useApi = () => {
  const context = useContext(ApiContext);
  if (context == undefined) {
    throw new Error("Api isn't defined?");
  }
  return context;
};
