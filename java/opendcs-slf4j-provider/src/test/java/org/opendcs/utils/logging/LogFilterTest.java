package org.opendcs.utils.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LogFilterTest
{
	@Test
	void testCanLog() throws IOException
	{
		Path logFilterFile = Files.createTempFile("OpenDCS", "UnitTest-testCanLog");
		Files.write(logFilterFile, LogFilterTest.class.getName().getBytes());
		LogFilter logFilter = new LogFilter(logFilterFile.toString());
		assertTrue(logFilter.canLog("HelloWorld"));
		assertFalse(logFilter.canLog(LogFilterTest.class.getName()));
	}

	@Test
	void testCanLogEmptyString() throws IOException
	{
		Path logFilterFile = Files.createTempFile("OpenDCS", "UnitTest-testCanLogEmptyString");
		String filterWithBlankLine = LogFilterTest.class.getName() + System.lineSeparator() + System.lineSeparator();
		Files.write(logFilterFile, filterWithBlankLine.getBytes());
		LogFilter logFilter = new LogFilter(logFilterFile.toString());
		assertTrue(logFilter.canLog("HelloWorld"));
		assertFalse(logFilter.canLog(LogFilterTest.class.getName()));
	}
}
