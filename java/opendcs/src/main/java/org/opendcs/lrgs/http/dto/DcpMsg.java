package org.opendcs.lrgs.http.dto;

import java.time.ZonedDateTime;

public record DcpMsg(String id, DataSource dataSource, ZonedDateTime retrievedTime, String msg)
{

}
