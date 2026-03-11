import type { Configuration } from "opendcs-api";
import { createConfiguration, ServerConfiguration } from "opendcs-api";
import { createContext, useContext, type SetStateAction } from "react";

export interface ApiContextType {
  conf: Configuration;
  org: string;
  setOrg: React.Dispatch<React.SetStateAction<string>>;
}

export const defaultValue: ApiContextType = {
  conf: createConfiguration({
    baseServer: new ServerConfiguration("/odcsapi", {}),
  }),
  org: window.localStorage.getItem("org") || "",
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  setOrg: (_value: SetStateAction<string>): void => {
    window.localStorage.setItem("org", _value.toString());
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
