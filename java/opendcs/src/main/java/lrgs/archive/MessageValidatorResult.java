package lrgs.archive;

import java.util.Date;

import decodes.util.PdtEntry;
import lrgs.common.DcpMsg;
import lrgs.lrgsmain.LrgsInputInterface;

public record MessageValidatorResult(char failureCode, String explanation, DcpMsg msg, LrgsInputInterface src,
                                     Date t, PdtEntry pdtEntry)
{    
}
