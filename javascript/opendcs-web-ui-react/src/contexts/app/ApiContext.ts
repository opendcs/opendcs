import type { Configuration } from "opendcs-api";
import { createConfiguration, ServerConfiguration } from "opendcs-api";
import { createContext, useContext, type SetStateAction } from "react";

export type ApiSetup = {
  conf: Configuration;
  org: string | undefined;
};

export interface ApiContextType {
  conf: Configuration;
  org: string;
  setOrg: React.Dispatch<React.SetStateAction<string | undefined>>;
}

export const defaultValue: ApiContextType = {
  conf: createConfiguration({
    baseServer: new ServerConfiguration("/odcsapi", {}),
  }),
  org: "",
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  setOrg: (_value: SetStateAction<string | undefined>): void => {},
};

export const ApiContext = createContext<ApiContextType>(defaultValue);

export const useApi = () => {
  const context = useContext(ApiContext);
  if (context == undefined) {
    throw new Error("Api isn't defined?");
  }
  return context;
};
