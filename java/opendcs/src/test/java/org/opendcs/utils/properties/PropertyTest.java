package org.opendcs.utils.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

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
}
