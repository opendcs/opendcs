package decodes.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;
import java.util.TimeZone;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import decodes.datasource.RawMessage;
import decodes.decoder.DecodedMessage;
import fixtures.DecodesHelper;

/**
 * Medium-weight decoding test: decodes a raw message with DecodesScript, then runs an
 * OutputFormatter and compares its whole text output to an .expected file.
 *
 * Each test set uses the same input files as DecodesScriptSampleTest (.decodescript,
 * .sensors, .input_*), plus two extra files:
 *   - {name}.formatter   Properties file. "class" names the formatter; "timezone"
 *                        (optional) sets its TimeZone; other keys are passed as
 *                        properties to the formatter.
 *   - {name}.expected    Golden text for whole-string comparison.
 *
 * A test set is picked up by this test only if its .expected file exists. Test sets
 * with only .assertions are picked up by DecodesScriptSampleTest instead.
 */
final class DecodesScriptFormatterTest
{
    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures.DecodesHelper#decodesFormatterTestSets")
    void test_formatted_output(String testName, DecodesScript script, RawMessage rawMessage,
                               DecodedMessage decodedMessage, String formatterClass,
                               Properties formatterProps, TimeZone timezone, String expected) throws Exception
    {
        String actual = DecodesHelper.formatDecodedMessage(decodedMessage, formatterClass, formatterProps, timezone);
        assertEquals(expected, actual, "Formatter output doesn't match golden for " + testName);
    }
}
