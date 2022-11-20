package decodes.db;

import static org.junit.jupiter.api.Assertions.*;

import decodes.datasource.EdlPMParser;
import decodes.datasource.RawMessage;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecodedSample;
import decodes.util.ResourceFactory;
import fixtures.DecodesHelper;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class DecodesScriptSampleTest {

    @BeforeAll
    public static void setup() throws Exception {
        ResourceFactory.instance().initializeFunctionList();
    }

    @ParameterizedTest
    @MethodSource("fixtures.DecodesHelper#decodesTestSets")
    void test_delimiter(final String testName, final DecodesScript script,
                        final RawMessage rawMessage, final DecodedMessage decodedMessage,
                        final ArrayList<DecodesHelper.DecodesAssertion> assertions) throws Exception {        
        assertions.forEach(a -> {
           fail();
            
        });
    }
}
