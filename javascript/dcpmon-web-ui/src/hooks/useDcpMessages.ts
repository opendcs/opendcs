import { useQuery } from "@tanstack/react-query";
import { getDcpMessages } from "../api";

export function useDcpMessages(dcpAddress: string, enabled: boolean) {
  return useQuery({
    queryKey: ["dcpmon", "messages", dcpAddress],
    queryFn: () => getDcpMessages(dcpAddress),
    enabled,
  });
}
