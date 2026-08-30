import { useQuery } from "@tanstack/react-query";
import { getDataGroups } from "../api";

export function useDataGroups() {
  return useQuery({
    queryKey: ["dcpmon", "data-groups"],
    queryFn: getDataGroups,
  });
}
