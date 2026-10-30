package org.opendcs.decodes.routing;

import java.util.function.Function;

import org.opendcs.decodes.api.DataMessage;
import org.opendcs.util.Result;

import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecoderException;

/**
 * Determines what to do with a given DataMessage instance from the Data Source.
 * Such as pass through, or decoding
 * MessageProcessor
 */
public class MessageProcessor implements Function<DataMessage, Result<DecodedMessage, DecoderException>>
{
    private final boolean attemptDecode;

    public MessageProcessor(boolean attemptDecode)
    {
        this.attemptDecode = attemptDecode;
    }

    @Override
    public Result<DecodedMessage, DecoderException> apply(DataMessage msg)
    {
        return switch (msg)
        {
          case DecodedMessage dm -> Result.success(dm);
          case RawMessage rm when attemptDecode -> decodeMessage(rm);
          case RawMessage rm -> noDecode(rm); // need to wrap in DecodedMessage but
                                              // we don't actually need to do anything.
          default -> Result.failure(new DecoderException("Unknown message type " + msg.getClass()));
        };
    }


    private Result<DecodedMessage, DecoderException> noDecode(RawMessage rm)
    {
        try
        {
            return Result.success(new DecodedMessage(rm, false));
        }
        catch (UnknownPlatformException ex)
        {
            return Result.failure(new DecoderException("Could not decode message", ex));
        }
    }

    /**
     *
     * @param rm
     * @return
     */
    private Result<DecodedMessage, DecoderException> decodeMessage(RawMessage rm)
    {
        try
        {
            return Result.success(new DecodedMessage(rm, false));
        }
        catch (UnknownPlatformException ex)
        {
            return Result.failure(new DecoderException("Could not decode message", ex));
        }
    }
}