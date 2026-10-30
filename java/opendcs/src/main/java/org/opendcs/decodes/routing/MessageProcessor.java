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

    /**
     * Determine and performance required decoding operations.
     * @param msg RawMessage or DecodedMessage to either process or pass-through.
     * @return DecodedMessage or reason for failure
     */
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

    /**
     * Wrap the case of providing only a RawMessage inside of a DecodedMessage instance.
     * @param rm the RawMessage
     * @return the DecodedMessage wrapped RawMessage, or the extremely unlikey failure result.
     */
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
     * TODO: Attempt to Decode the actual message.
     * At this time we're still building the general application structure and revising
     * how data is retrieved by the various components, at this time is not practical
     * to actually decode the mssage.
     * @param rm The RawMessage
     * @return The DecodedMessage, or the reason for failure.
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