package decodes.aesrd;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AepTestClient
{
	private static final Logger LOGGER = Logger.getLogger(AepTestClient.class.getName());

	@ParameterizedTest
	@MethodSource("decodes.aesrd.AepTestClient#getStations")
	@Disabled("Tests currently not passing. Not sure if due to legacy addresses or needing VPN access.")
	void aepTestClient(String name, String host, Integer port) throws IOException
	{
		LOGGER.info("Trying " + name + " " + host + ":" + port);
		try(Socket sock = new Socket(host, port))
		{
			assertNotNull(sock.getInputStream());
			LOGGER.info("Success!");
		}
	}

	private static Stream<Arguments> getStations()
	{
		return Stream.of(
				Arguments.of("ALBE", "7802661981.eairlink.com", 6785),
				Arguments.of("ATH2", "74.198.191.114", 6785),
				Arguments.of("AURO", "96.1.79.140", 6785),
				Arguments.of("BALL", "7802316519.eairlink.com", 6785),
				Arguments.of("BARR", "184.151.142.29", 6785),
				Arguments.of("BUSB", "7802661779.eairlink.com", 6785),
				Arguments.of("CALG", "74.198.246.96", 6785),
				Arguments.of("CAMR", "74.198.224.136", 6785),
				Arguments.of("CRET", "7802662489.eairlink.com", 6785),
				Arguments.of("DUPR", "5873356968.eairlink.com", 6785),
				Arguments.of("HAWK", "7802661686.eairlink.com", 6785),
				Arguments.of("KINS", "7802389742.eairlink.com", 6785),
				Arguments.of("MUNI", "74.198.230.159", 6785),
				Arguments.of("MVIL", "7802661461.eairlink.com", 6785),
				Arguments.of("NORD", "184.151.139.176", 6785),
				Arguments.of("RADW", "7802662453.eairlink.com", 6785),
				Arguments.of("RAIN", "7802647159.eairlink.com", 6785),
				Arguments.of("REDI", "5873363150.eairlink.com", 6785),
				Arguments.of("SHEN", "7808937160.eairlink.com", 6785),
				Arguments.of("STAL", "7802350197.eairlink.com", 6785),
				Arguments.of("STEE", "96.1.78.134", 6785)
		);
	}
}

