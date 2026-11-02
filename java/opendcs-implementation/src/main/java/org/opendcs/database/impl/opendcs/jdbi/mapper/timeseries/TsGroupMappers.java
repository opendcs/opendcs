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
package org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries;

import org.opendcs.database.model.mappers.datatype.DataTypeMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteNameMapper;

/**
 * The set of mappers used to read a group, its sub-groups and their members, each
 * with the table alias it's query uses.
 */
public record TsGroupMappers(TsGroupMapper groupMapper, OpenDcsTimeSeriesIdentifierMapper tsiMapper,
                             DataTypeMapper dtMapper, OpenDcsSiteMapper siteMapper,
                             OpenDcsSiteNameMapper siteNameMapper)
{
    public static TsGroupMappers withPrefixes()
    {
        return new TsGroupMappers(TsGroupMapper.withPrefix("tg"),
                                  OpenDcsTimeSeriesIdentifierMapper.withPrefix("tsi"),
                                  DataTypeMapper.withPrefix("dt"),
                                  OpenDcsSiteMapper.withPrefix("s"),
                                  OpenDcsSiteNameMapper.withPrefix("sn"));
    }
}
