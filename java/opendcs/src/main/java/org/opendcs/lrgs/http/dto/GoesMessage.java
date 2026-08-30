package org.opendcs.lrgs.http.dto;

/** A GOES message represented by the DDS-over-HTTP contract. */
public record GoesMessage(
    String messageType,
    String dcpAddress,
    String receiveTime,
    DataSource dataSource,
    String cType,
    String arm,
    String eirp,
    String frequency,
    String modulation,
    String quality,
    String channel,
    String downlink,
    int charlen,
    String data)
{
}
