package org.opendcs.fixtures;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class TypedParameterResolver<T> implements ParameterResolver{
    private T parameter;


    public TypedParameterResolver(T parameter)
    {
        this.parameter = parameter;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException
    {
        Object ret = null;
        if(parameterContext.getParameter().getType() == parameter.getClass())
        {
            ret = parameter;
        }
        return ret;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, 
                                     ExtensionContext extensionContext) throws ParameterResolutionException 
    {
        return parameterContext.getParameter().getType() == parameter.getClass();
    }
    
}
