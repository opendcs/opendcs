import { DCPMON_API_BASE_URL } from "./constants";
import type { DcpMessageResponse, StatusGroupSummary } from "./types";

async function readJson<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

export function getStatusGroupSummary(group: string): Promise<StatusGroupSummary> {
  const params = new URLSearchParams({ group });
  return readJson<StatusGroupSummary>(
    `${DCPMON_API_BASE_URL}/data/summary?${params.toString()}`,
  );
}

export function getDcpMessages(dcpAddress: string): Promise<DcpMessageResponse> {
  const params = new URLSearchParams({ source: "goes", dcpAddress });
  return readJson<DcpMessageResponse>(
    `${DCPMON_API_BASE_URL}/data/query?${params.toString()}`,
  );
}
