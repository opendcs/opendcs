package integration.cove.azul;

import java.io.File;
import java.io.IOException;

import ilex.net.BasicClient;
import ilex.util.FileUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Need to stand up mock server in order to receive iridium replay. Need to get iridium test directory setup")
class TestIridiumReplay
{
	private static String host;
	private static int port = 10800;
	private static File directory;

	@BeforeAll
	public static void setup()
	{
		host = System.getProperty("cove.azul.iridium.replay.test.host", "localhost:10800");
		int colon = host.indexOf(':');
		if(colon >= 0)
		{
			port = Integer.parseInt(host.substring(colon + 1));
			host = host.substring(0, colon);
		}

		String iridiumTestDir = System.getProperty("cove.azul.iridium.replay.test.directory", "iridium");
		directory = new File(iridiumTestDir);
		assertTrue(directory.isDirectory(), () -> "'" + iridiumTestDir + "' is not a directory.");
	}

	@Test
	void testIridiumReplay() throws Exception
	{
		File[] files = directory.listFiles();
		for(File file : files)
		{
			processFile(file);
			Thread.sleep(1000L);
		}
	}

	private void processFile(File file) throws IOException
	{
		System.out.println("Processing file '" + file.getPath() + "'");
		byte[] data = FileUtil.getfileBytes(file);

		// xmit time is in Unix time_t format (big endian). Replace it with current time.
		int nowtt = (int) (System.currentTimeMillis() / 1000L);
		data[30] = (byte) ((nowtt >> 24) & 0xff);
		data[31] = (byte) ((nowtt >> 16) & 0xff);
		data[32] = (byte) ((nowtt >> 8) & 0xff);
		data[33] = (byte) (nowtt & 0xff);

		BasicClient client = new BasicClient(host, port);
		client.connect();
		assertDoesNotThrow(() -> client.sendData(data));
		client.disconnect();
	}

}
