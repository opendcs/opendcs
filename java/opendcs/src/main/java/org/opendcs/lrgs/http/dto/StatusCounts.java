package org.opendcs.lrgs.http.dto;

public record StatusCounts(int missing, int partial, int parity, int complete, int unknown)
{
}
