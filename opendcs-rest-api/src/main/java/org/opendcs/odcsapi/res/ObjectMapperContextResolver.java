/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.res;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectMapperContextResolver.class);
	private final ObjectMapper mapper;


    public ObjectMapperContextResolver() 
    {
        this.mapper = createObjectMapper();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) 
    {
        return mapper;
    }

    private ObjectMapper createObjectMapper() 
    {
        String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'[z]";
        String timezone = "UTC";
        LOGGER.info("Creating object map using date format '{}' and timezone '{}'", dateFormat, timezone);
        ObjectMapper objMap = new ObjectMapper();
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        objMap.setDateFormat(sdf);
        return objMap;
    }
}
