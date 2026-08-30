package org.opendcs.lrgs.http.dto;

import java.util.List;

/** Envelope returned by DDS message retrieval operations. */
public record DcpMessages(
    int total,
    List<GoesMessage> messages,
    DataSource dataSource,
    List<DcpTransmission> transmissionSlots)
{
}
