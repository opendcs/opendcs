import { useQuery } from "@tanstack/react-query";
import { getStatusGroupSummary } from "../api";

export function useStatusGroupSummary(group: string) {
  return useQuery({
    queryKey: ["dcpmon", "status-group-summary", group],
    queryFn: () => getStatusGroupSummary(group),
  });
}
