package integration.lrgs.domsatrecv;

import lrgs.domsatrecv.DomsatSangoma;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Requires WinDomsatSangoma.dll on java.library.path")
final class TestDomstatSangoma
{

	private static DomsatSangoma ds;

	@BeforeAll
	public static void setup()
	{
		ds = new DomsatSangoma();
		ds.init();
		assertTrue(ds.setEnabled(true), () -> "Enable failed -- see log messages.");
	}

	@AfterAll
	public static void tearDown()
	{
		ds.shutdown();
	}

	@RepeatedTest(value = 20, name = RepeatedTest.LONG_DISPLAY_NAME)
	void domsatFranklin()
	{
		byte[] packet = new byte[1024];
		int len = ds.getPacket(packet);
		assertNotEquals(0, len, () -> "Timeout");
		assertNotEquals(-1, len, () -> "Recoverable Error: " + ds.getErrorMsg());
		assertNotEquals(-2, len, () -> "Fatal Error: " + ds.getErrorMsg());
		System.out.println("Frame received, len=" + len);
		System.out.println(" "
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
