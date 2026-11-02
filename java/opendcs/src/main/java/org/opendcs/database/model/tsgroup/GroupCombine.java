/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package org.opendcs.database.model.tsgroup;

/**
 * How a sub-group is combined with the group that references it: the
 * tsdb_group_member_group.include_group column.
 */
public enum GroupCombine
{
    ADD('A'),
    SUBTRACT('S'),
    INTERSECT('I');

    private final char value;

    GroupCombine(char value)
    {
        this.value = value;
    }

    /** @return the value stored in the include_group column. */
    public char value()
    {
        return value;
    }

    /**
     * 'F' is how older databases spell subtract. Anything unrecognized adds, which is
     * also what a null or blank column means.
     */
    public static GroupCombine fromValue(char value)
    {
        return switch (Character.toUpperCase(value))
        {
            case 'S', 'F' -> SUBTRACT;
            case 'I' -> INTERSECT;
            default -> ADD;
        };
    }

    public static GroupCombine fromValue(String value)
    {
        return value == null || value.isBlank() ? ADD : fromValue(value.charAt(0));
    }
}
