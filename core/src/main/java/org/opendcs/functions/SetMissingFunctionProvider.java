package org.opendcs.functions;

import org.opendcs.spi.decodes.DecodesFunctionProvider;

import decodes.decoder.DecodesFunction;
import decodes.decoder.SetMissingFunction;

public class SetMissingFunctionProvider implements DecodesFunctionProvider
{

    @Override
    public String getName()
    {
        return SetMissingFunction.module;
    }

    @Override
    public DecodesFunction createInstance() 
    {
        return new SetMissingFunction(); 
    }

}
