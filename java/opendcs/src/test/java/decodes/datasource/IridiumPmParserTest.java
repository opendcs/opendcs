/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/

package decodes.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class IridiumPmParserTest
{
    @Test
    void test_good_message() throws Exception
    {
        final String goodMessage =
                        "ID=300434062451390,TIME=25174235957,STAT=00,MO=03515,MT=00000,CDR=942C5694,LAT=39.90232,LON=-75.23737,RAD=9 IE:0200B5 1,97,594:\n" + //
                        "06/23/2025,23:55:00,LOWERWTEMP,-99999.00,,B\n" + //
                        "06/23/2025,23:55:00,LOWERSC,-99999.00,,B\n" + //
                        "06/23/2025,23:55:00,LOWERDEPTH,-99999.00,,B\n" + //
                        "06/23/2025,23:55:00,BATTVT,12.95,V,G";
        final RawMessage rawMsg = new RawMessage(goodMessage.getBytes());
        final IridiumPMParser pmParser = new IridiumPMParser();
        pmParser.parsePerformanceMeasurements(rawMsg);
        assertEquals("300434062451390",rawMsg.getMediumId(), "Unable to parse performance measurements.");
        assertEquals(39.90232, rawMsg.getPM(IridiumPMParser.LATITUDE).getDoubleValue(), .0001, "Unable to retrieve latitude from header.");

    }

    // This test is intentionally simple. We want to make sure data either parses without error
    // or the correct Exception is thrown.
    // Future work that cleans up the PMParsing implementations should benefit from this test
    // making sure the correct exception is thrown on error.
    @ParameterizedTest
    @MethodSource("get_messages")
    void test_messages_parse_or_throw_correct_error(String msg, int line)
    {

        final RawMessage rawMsg = new RawMessage(msg.getBytes());
        final IridiumPMParser pmParser = new IridiumPMParser();
        try
        {
            pmParser.parsePerformanceMeasurements(rawMsg);
        }
        catch (HeaderParseException ex)
        {
            /* expected */
        }
        catch (Throwable ex)
        {
            fail("Message on line " + line + " did not process correctly.", ex);
        }
    }

    /**
     * While the source data file is now sized more reasonably, I had to process 3 months
     * of data to identify the problem messages need to fix a problem. Kept running out of memory.
     * Doesn't make sense to just delete all this since things will likely expand anyways.
     */
    private static class IridiumMessageIterator implements Iterator<Arguments>
    {
        private InputStream is;
        private BufferedReader reader;
        private int lineNumber = 0;
        private boolean atEnd = false;
        public IridiumMessageIterator(InputStream is)
        {
            this.is = is;
            reader = new BufferedReader(new InputStreamReader(is));
        }

        @Override
        public boolean hasNext()
        {
            return !atEnd;
        }

        @Override
        public Arguments next() {
            Arguments ret = null;
            String line = null;
            int currentLine = lineNumber;
            StringBuilder current = new StringBuilder();

            try
            {
                while ((line = reader.readLine()) != null && !line.contains("----NEXT----"))
                {
                    current.append(line+System.lineSeparator());
                    lineNumber++;
                }
                ret = Arguments.of(current.toString(), currentLine);
                if (line == null)
                {
                    atEnd = true;
                }
            }
            catch (IOException e)
            {
                ret = Arguments.of(e, lineNumber);
            }
            return ret;

        }

        public void close() throws IOException
        {
            reader.close();
        }
    }

    public static Stream<Arguments> get_messages() throws IOException
    {
        URL dataFile = IridiumPmParserTest.class.getResource("/decodes/datasource/iridium.small.txt");
        final InputStream is = dataFile.openStream();
        IridiumMessageIterator iter = new IridiumMessageIterator(is);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED), false)
                            .onClose(() -> {
                                try {
                                    iter.close();
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            });

    }
}
