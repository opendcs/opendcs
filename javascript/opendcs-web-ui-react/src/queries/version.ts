import { useQuery } from "@tanstack/react-query";
import { versionKeys } from "./keys";

export interface ApiVersion {
  version: string;
  commitHash: string;
}

export const fetchVersion = (): Promise<ApiVersion> =>
  fetch("/odcsapi/version").then((res) => {
    if (!res.ok) throw new Error(`Version fetch failed: ${res.status}`);
    return res.json() as Promise<ApiVersion>;
  });

export const useVersionQuery = () =>
  useQuery<ApiVersion>({
    queryKey: versionKeys.all(),
    queryFn: fetchVersion,
    staleTime: Infinity,
  });
