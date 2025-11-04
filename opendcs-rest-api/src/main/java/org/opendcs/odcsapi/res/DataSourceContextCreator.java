/*
 *  Copyright 2024 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

@WebListener
public final class DataSourceContextCreator implements ServletContextListener
{
	public static final String DATA_SOURCE_ATTRIBUTE_KEY = "OPENDCS_DATA_SOURCE";

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent)
	{
		String lookupName = "jdbc/opentsdb";
		try
		{
			Context initialCtx = new InitialContext();
			Context envCtx = (Context) initialCtx.lookup("java:comp/env");
			DataSource dataSource = (DataSource) envCtx.lookup(lookupName);
			servletContextEvent.getServletContext().setAttribute(DATA_SOURCE_ATTRIBUTE_KEY, dataSource);
		}
		catch(NamingException ex)
		{
			throw new IllegalStateException("ServletContext missing data source access via name: " + lookupName, ex);
		}
	}
}
