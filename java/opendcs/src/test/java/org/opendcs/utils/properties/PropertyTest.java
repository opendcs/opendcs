package org.opendcs.utils.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Enumeration;

import org.junit.jupiter.api.Test;

import ilex.util.HasProperties;

class PropertyTest
{
    @Test
    void test_get_default()
    {
        final Integer defaultValue = 42;

        final Property<Integer> prop = Property.property("testProp", Integer.class)
                                               .withDefaultValue(defaultValue)
                                               .build();

        final Integer actual = prop.get();
        assertEquals(defaultValue, actual);
    }


    @Test
    void test_second_source()
    {
        var source1 = new SimpleSource(null);
        var source2 = new SimpleSource("theValue");

        var defaultValue = "DefaultValue";

        final var prop = Property.property("testProp", String.class)
                                 .withDefaultValue(defaultValue)
                                 .withSources(source1, source2)
                                 .build();

        assertEquals("theValue", prop.get());
    }



    private static final class SimpleSource implements HasProperties
    {
        final String value;

        public SimpleSource(String value)
        {
            this.value = value;
        }

        @Override
        public void setProperty(String name, String value)
        {
            /* unused */
        }

        @Override
        public String getProperty(String name)
        {
            return value;
        }

        @Override
        public Enumeration getPropertyNames()
        {
            return Collections.emptyEnumeration();
        }

        @Override
        public void rmProperty(String name)
        {
            /* unused */
        }
    }
}
