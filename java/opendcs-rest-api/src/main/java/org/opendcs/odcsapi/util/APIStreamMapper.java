package org.opendcs.odcsapi.util;

import java.util.List;
import java.util.stream.Collectors;

import decodes.db.DatabaseException;

public final class APIStreamMapper
{
	private APIStreamMapper()
	{
	}

	public static <T, R> List<R> mapList(List<T> list, Class<R> targetClass)
	{
		return list.stream().map(value ->
		{
			try
			{
				return targetClass.cast(DTOMappers.map(value, targetClass));
			}
			catch(DatabaseException | ClassCastException e)
			{
				throw new RuntimeException(
						String.format("Failed to map parameter: %s", value.toString()),
						e);
			}
		}).collect(Collectors.toList());
	}
}
