package org.opendcs.lrgs.http.dto;

/** One expected self-timed transmission window and its matching message, when received. */
public record DcpTransmission(
    String expectedTime,
    String status,
    GoesMessage message)
{
}
