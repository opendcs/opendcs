package org.opendcs.lrgs.http.dto;

import java.util.List;

public record DcpSummary(
    List<DcpIdentifier> identifiers,
    String status,
    int messageTotal,
    Integer expectedMessageTotal,
    int parityCount,
    boolean lowBattery,
    Boolean gpsSync)
{
}
