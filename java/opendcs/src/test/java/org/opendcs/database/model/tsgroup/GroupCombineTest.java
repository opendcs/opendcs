package org.opendcs.database.model.tsgroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GroupCombineTest
{
    @Test
    void test_stored_values()
    {
        assertEquals('A', GroupCombine.ADD.value());
        assertEquals('S', GroupCombine.SUBTRACT.value());
        assertEquals('I', GroupCombine.INTERSECT.value());
    }

    @Test
    void test_every_value_round_trips()
    {
        for (GroupCombine combine : GroupCombine.values())
        {
            assertEquals(combine, GroupCombine.fromValue(combine.value()));
        }
    }

    @Test
    void test_from_character()
    {
        assertEquals(GroupCombine.ADD, GroupCombine.fromValue('A'));
        assertEquals(GroupCombine.ADD, GroupCombine.fromValue('a'));
        assertEquals(GroupCombine.SUBTRACT, GroupCombine.fromValue('S'));
        assertEquals(GroupCombine.SUBTRACT, GroupCombine.fromValue('s'));
        assertEquals(GroupCombine.INTERSECT, GroupCombine.fromValue('I'));
        assertEquals(GroupCombine.INTERSECT, GroupCombine.fromValue('i'));
    }

    @Test
    void test_f_is_the_older_subtract()
    {
        assertEquals(GroupCombine.SUBTRACT, GroupCombine.fromValue('F'));
        assertEquals(GroupCombine.SUBTRACT, GroupCombine.fromValue('f'));
    }

    @Test
    void test_anything_else_adds()
    {
        assertEquals(GroupCombine.ADD, GroupCombine.fromValue('X'));
        assertEquals(GroupCombine.ADD, GroupCombine.fromValue(' '));
    }

    @Test
    void test_from_string()
    {
        assertEquals(GroupCombine.INTERSECT, GroupCombine.fromValue("I"));
        assertEquals(GroupCombine.SUBTRACT, GroupCombine.fromValue("s"));
        assertEquals(GroupCombine.ADD, GroupCombine.fromValue((String) null));
        assertEquals(GroupCombine.ADD, GroupCombine.fromValue(""));
        assertEquals(GroupCombine.ADD, GroupCombine.fromValue("   "));
    }
}
