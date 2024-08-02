package org.opendcs.decodes.functions;

import org.opendcs.spi.decodes.DecodesFunctionProvider;

import decodes.decoder.DecodesFunction;
import decodes.decoder.ShefProcess;

public class ShefProcessProvider implements DecodesFunctionProvider
{

    @Override
    public String getName()
    {
        return ShefProcess.module;
    }

    @Override
    public DecodesFunction createInstance() 
    {
        return new ShefProcess();
    }
    
}
