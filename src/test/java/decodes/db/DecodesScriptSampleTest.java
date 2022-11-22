package decodes.db;

import static org.junit.jupiter.api.Assertions.*;

import decodes.datasource.EdlPMParser;
import decodes.datasource.RawMessage;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecodedSample;
import decodes.util.ResourceFactory;
import fixtures.DecodesHelper;
import ilex.var.IFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import ilex.var.VariableType;

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
           DecodedSample actual = DecodesHelper.sampleFor(a.getSensor(), a.getTime(), script.getDecodedSamples());
           Variable ev = a.getExpectedValue();
           TimedVariable av = actual.getSample();
           try
           {
                switch (ev.getNativeType())
                {
                        case VariableType.STRING:
                        {
                            final String avS = ((av.getFlags() & IFlags.IS_MISSING) == 0) 
                                                ? av.getStringValue()
                                                : "m";
                            assertEquals(ev.getStringValue(),avS,a.getMessage());
                            break;
                        }
                        case VariableType.DOUBLE:
                        {
                            assertEquals(ev.getDoubleValue(),av.getDoubleValue(),a.getPrecision(),a.getMessage());
                            break;
                        }
                }
           }
           catch(Exception ex)
           {
            ex.printStackTrace(System.err);
            fail("exception thrown");
           }
        });
    }
}
