package integration.lrgs.domsatrecv;

import java.util.logging.Logger;

import lrgs.domsatrecv.DomsatDpc;
import lrgs.lrgsmain.LrgsConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Requires DOMSAT DPC installed on localhost")
final class TestDomsat
{
	private static final Logger LOGGER = Logger.getLogger(TestDomsat.class.getName());
	private static DomsatDpc ds;

	@BeforeAll
	public static void setup()
	{
		String host = System.getProperty("domsat.dpc.test.host", "localhost");
		int port = Integer.getInteger("domsat.dpc.test.post", 9000);
		LrgsConfig.instance().dpcHost = host;
		LrgsConfig.instance().dpcPort = port;
		ds = new DomsatDpc();
		ds.init();
		assertTrue(ds.setEnabled(true), () -> "Enable failed -- see log messages.");
	}

	@AfterAll
	public static void tearDown()
	{
		ds.shutdown();
	}

	@RepeatedTest(value = 20, name = RepeatedTest.LONG_DISPLAY_NAME)
	void dpcDomsat() throws Exception
	{
		byte packet[] = new byte[1024];
		int len = ds.getPacket(packet);

		assertNotEquals(0, len, () -> "Timeout");
		assertNotEquals(-1, len, () -> "Recoverable Error: " + ds.getErrorMsg());
		assertNotEquals(-2, len, () -> "Fatal Error: " + ds.getErrorMsg());
		while(len == -3)
		{
			Thread.sleep(100L);
			len = ds.getPacket(packet);
		}
		LOGGER.info("Frame received, len=" + len);
		LOGGER.info(" "
				+ Integer.toHexString((int) packet[0] & 0xff) + ", "
				+ Integer.toHexString((int) packet[1] & 0xff) + ", "
				+ Integer.toHexString((int) packet[2] & 0xff) + ", "
				+ Integer.toHexString((int) packet[3] & 0xff) + ", "
				+ Integer.toHexString((int) packet[4] & 0xff) + ", "
				+ Integer.toHexString((int) packet[5] & 0xff) + ", "
				+ Integer.toHexString((int) packet[6] & 0xff) + ", "
				+ Integer.toHexString((int) packet[7] & 0xff) + ", ... "
				+ Integer.toHexString((int) packet[len - 2] & 0xff) + ", "
				+ Integer.toHexString((int) packet[len - 1] & 0xff));
	}
}
