package org.opendcs.app;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

public class ArgsArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<ArgsSource>
{
    private String[] args;
    @Override
    public void accept(ArgsSource t)
    {
        args = t.value();
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        Object arg = (Object)args;
        return Stream.of(Arguments.of(arg));
    }

}
