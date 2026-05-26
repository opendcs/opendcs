import type { ApiTransportMedium } from "opendcs-api";

/**
 * Composite identity for a transport medium row. Neither `mediumType` nor
 * `mediumId` is unique on its own (e.g. a platform may carry GOES self-timed
 * and GOES random with the same DCP address), so the table and reducer key
 * rows by the pair.
 */
export function transportKey(medium: Partial<ApiTransportMedium>): string {
  return `${medium.mediumType ?? ""}::${medium.mediumId ?? ""}`;
}
