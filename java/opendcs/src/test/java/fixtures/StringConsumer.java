package fixtures;

import java.util.Properties;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.decoder.DecodedMessage;

/**
 * In-memory DataConsumer that captures formatter output as a String.
 *
 * Used by DecodesScriptFormatterTest to test Decoding and Formatting.
 */
public final class StringConsumer extends DataConsumer
{
    private final StringBuilder sb = new StringBuilder();

    @Override
    public void open(String consumerArg, Properties props) throws DataConsumerException
    {
    }

    @Override
    public void close()
    {
    }

    @Override
    public void startMessage(DecodedMessage msg) throws DataConsumerException
    {
    }

    @Override
    public void println(String line)
    {
        sb.append(line).append('\n');
    }

    @Override
    public void endMessage()
    {
    }

    public String getOutput()
    {
        return sb.toString();
    }
}
