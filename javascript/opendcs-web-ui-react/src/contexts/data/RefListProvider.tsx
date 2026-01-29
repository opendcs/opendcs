import type { ReactNode } from "react";
import { defaultValue, RefListContext } from "./RefListContext";

interface ProviderProps {
  children: ReactNode;
}

export const RefListProvider = ({ children }: ProviderProps) => {
  return <RefListContext value={defaultValue}>{children}</RefListContext>;
};
