package org.opendcs.decodes.functions;

import org.opendcs.spi.decodes.DecodesFunctionProvider;

import decodes.decoder.DecodesFunction;
import decodes.decoder.ReplaceValueFunction;

public class ReplaceValueFunctionProvider implements DecodesFunctionProvider
{

    @Override
    public String getName()
    {
        return ReplaceValueFunction.module;
    }

    @Override
    public DecodesFunction createInstance() 
    {
        return new ReplaceValueFunction(); 
    }

}
