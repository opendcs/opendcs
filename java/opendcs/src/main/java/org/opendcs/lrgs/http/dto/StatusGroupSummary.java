package org.opendcs.lrgs.http.dto;

import java.util.Map;

public record StatusGroupSummary(
    String timestamp,
    int durationHours,
    StatusCounts counts,
    Map<String, DcpSummary> dcpSummaries)
{
}
