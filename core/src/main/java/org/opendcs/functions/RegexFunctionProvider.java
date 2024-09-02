package org.opendcs.functions;

import org.opendcs.spi.decodes.DecodesFunctionProvider;

import decodes.decoder.DecodesFunction;
import decodes.decoder.RegexFunction;

public class RegexFunctionProvider implements DecodesFunctionProvider
{

    @Override
    public String getName()
    {
        return RegexFunction.module;
    }

    @Override
    public DecodesFunction createInstance() 
    {
        return new RegexFunction(); 
    }

}
