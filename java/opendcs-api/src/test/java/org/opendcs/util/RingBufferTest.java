package org.opendcs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class RingBufferTest
{
    @Test
    void test_ring_wrapping()
    {
        RingBuffer<Integer> buffer = new RingBuffer<>(3);
        assertTrue(buffer.isEmpty());

        for (int i = 0; i < 3; i++)
        {
            buffer.add(i);
        }
        assertEquals(3, buffer.size());
        for (int i = 0; i < 3; i++)
        {
            assertEquals(i, buffer.get(i));
        }

        buffer.add(4);
        assertEquals(3, buffer.size());
        assertTrue(buffer.contains(4));
        assertNotEquals(0, buffer.get(0));
        ArrayList<Integer> toAdd = new ArrayList<>();
        toAdd.add(1);
        toAdd.add(2);
        toAdd.add(3);
        toAdd.add(4);
        buffer.addAll(toAdd);
        assertEquals(3, buffer.size());

    }
    

    @Test
    void test_ring_size_increase()
    {
        RingBuffer<Integer> buffer = new RingBuffer<>(2);
        buffer.add(0);
        buffer.add(1);
        buffer.add(2);
        assertEquals(2, buffer.size());
        assertEquals(1, buffer.get(0));
        buffer.setSize(5);
        buffer.add(3);
        assertEquals(3, buffer.size());
        assertEquals(1, buffer.get(0));
        assertEquals(3, buffer.get(buffer.size()-1));
        
    }

    @Test
    void test_ring_unsupported_opts_throw()
    {
        final RingBuffer<Integer> buffer = new RingBuffer<>(5);
        assertThrows(UnsupportedOperationException.class, () -> buffer.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> buffer.remove((Integer)0));
        assertThrows(UnsupportedOperationException.class, () -> buffer.addAll(0, null));
        assertThrows(UnsupportedOperationException.class, () -> buffer.removeAll(null));
        assertThrows(UnsupportedOperationException.class, () -> buffer.retainAll(null));
        assertThrows(UnsupportedOperationException.class, () -> buffer.clear());
        assertThrows(UnsupportedOperationException.class, () -> buffer.set(0,0));
    }
}
