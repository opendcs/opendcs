package org.opendcs.decodes.functions;

import org.opendcs.spi.decodes.DecodesFunctionProvider;

import decodes.decoder.DecodesFunction;
import decodes.decoder.ReplaceValueWithFunction;

public class ReplaceValueWithFunctionProvider implements DecodesFunctionProvider
{

    @Override
    public String getName()
    {
        return ReplaceValueWithFunction.module;
    }

    @Override
    public DecodesFunction createInstance() 
    {
        return new ReplaceValueWithFunction(); 
    }

}
