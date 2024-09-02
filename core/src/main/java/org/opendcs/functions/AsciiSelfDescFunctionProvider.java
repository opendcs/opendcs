package org.opendcs.functions;

import org.opendcs.spi.decodes.DecodesFunctionProvider;

import decodes.decoder.AsciiSelfDescFunction;
import decodes.decoder.DecodesFunction;

public class AsciiSelfDescFunctionProvider implements DecodesFunctionProvider
{

    @Override
    public String getName()
    {
        return AsciiSelfDescFunction.module;
    }

    @Override
    public DecodesFunction createInstance() 
    {
        return new AsciiSelfDescFunction(); 
    }

}
